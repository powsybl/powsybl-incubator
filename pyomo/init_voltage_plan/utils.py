import numpy as np
import pyomo.environ as pyo

# Count neighbours function
def nb_neighboursPV(net, PV_buses):
    neighbours_count = dict()
    buses = net.get_buses().index.tolist()
    for id_line, row in net.get_lines().iterrows():
        b1 = row["bus1_id"]
        b2 = row["bus2_id"]
        if b1 in PV_buses and b2 in buses:
            if b1 not in neighbours_count:
                neighbours_count[b1] = 1
            else:
                neighbours_count[b1] += 1
        if b2 in PV_buses and b1 in buses:
            if b2 not in neighbours_count:
                neighbours_count[b2] = 1
            else:
                neighbours_count[b2] += 1
    for id_tr, row in net.get_2_windings_transformers().iterrows():
        b1 = row["bus1_id"]
        b2 = row["bus2_id"]
        if b1 in PV_buses and b2 in buses:
            if b1 not in neighbours_count:
                neighbours_count[b1] = 1
            else:
                neighbours_count[b1] += 1
        if b2 in PV_buses and b1 in buses:
            if b2 not in neighbours_count:
                neighbours_count[b2] = 1
            else:
                neighbours_count[b2] += 1
    return neighbours_count
    
# Objectif function
def obj_expression(n, PQ_buses):
    def obj_expression_m(m):
        res = 0
        for id_b, row in n.get_buses().iterrows():
            if id_b not in PQ_buses:
                continue
            if not np.isnan(row["v_mag"]):
                v_pu_coeff = n.get_voltage_levels().at[row["voltage_level_id"], "nominal_v"]
                bus_v_pu = row["v_mag"] / v_pu_coeff # TO DO: to be replaced with pu getters
                res += (m.V[id_b] - bus_v_pu) * (m.V[id_b] - bus_v_pu)
        return res
    return obj_expression_m

# Active power balance
def active_power_balance_expr(n, buses, p_pu):
    def active_power_balance_expr_m(m,i):
        v_pu_coeff_i = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
        lhs = 0
        rhs = 0
        is_empty = True
        for id_line, row in n.get_lines().iterrows():
            if row["bus1_id"] == i and row["bus2_id"] in buses:
                is_empy = False
                j = row["bus2_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu = 1
                line_g1_pu = row[ "g1"] / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += ratio_tr_pu * m.V[i] * ( line_g1_pu * ratio_tr_pu * m.V[i] + line_y_pu * ratio_tr_pu * m.V[i] * pyo.sin(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.sin(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
            elif row["bus2_id"] == i and row["bus1_id"] in buses:
                is_empy = False
                j = row["bus1_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu = 1
                line_g2_pu = row["g2"] / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += 1 * m.V[i] * ( line_g2_pu * 1 * m.V[i] - line_y_pu * ratio_tr_pu * m.V[j] * pyo.sin(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.sin(line_ksi) )
        for id_tr, row in n.get_2_windings_transformers().iterrows():
            if row["bus1_id"] == i and row["bus2_id"] in buses:
                is_empy = False
                j = row["bus2_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U
                ratio_tr_pu_coeff = v_pu_coeff_j / v_pu_coeff_i
                ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += ratio_tr_pu * m.V[i] * (line_y_pu * ratio_tr_pu * m.V[i] * pyo.sin(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.sin(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
            elif row["bus2_id"] == i and row["bus1_id"] in buses:
                is_empy = False
                j = row["bus1_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu_coeff = v_pu_coeff_i / v_pu_coeff_j
                ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += 1 * m.V[i] * (- line_y_pu * ratio_tr_pu * m.V[j] * pyo.sin(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.sin(line_ksi) )
        for id_gen, row in n.get_generators().iterrows():
            if row["bus_id"] == i:
                rhs += row["target_p"] / p_pu # TO DO: to be replaced with pu getters
        for id_ld, row in n.get_loads().iterrows():
            if row["bus_id"] == i:
                rhs -= row["p0"] / p_pu # TO DO: to be replaced with pu getters
        if is_empty:
            return pyo.Constraint.Skip
        else:
            return lhs == rhs
    return active_power_balance_expr_m

# Reactive power balance for PQ buses
def reactive_power_balancePQ_expr(n, buses, p_pu):
    def reactive_power_balancePQ_expr_m(m,i):
        v_pu_coeff_i = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
        lhs = 0
        rhs = 0
        is_empty = True
        for id_line, row in n.get_lines().iterrows():
            if row["bus1_id"] == i and row["bus2_id"] in buses:
                is_empy = False
                j = row["bus2_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu = 1
                line_b1_pu = row[ "b1"] / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += ratio_tr_pu * m.V[i] * (-line_b1_pu * ratio_tr_pu * m.V[i] + line_y_pu * ratio_tr_pu * m.V[i] * pyo.cos(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.cos(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
            elif row["bus2_id"] == i and row["bus1_id"] in buses:
                is_empy = False
                j = row["bus1_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu = 1
                line_b2_pu = row["b2"] / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += 1 * m.V[i] * ( - line_b2_pu * 1 * m.V[i] - line_y_pu * ratio_tr_pu * m.V[j] * pyo.cos(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.cos(line_ksi) )
        for id_tr, row in n.get_2_windings_transformers().iterrows():
            if row["bus1_id"] == i and row["bus2_id"] in buses:
                is_empy = False
                j = row["bus2_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U
                ratio_tr_pu_coeff = v_pu_coeff_j / v_pu_coeff_i
                ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += ratio_tr_pu * m.V[i] * (line_y_pu * ratio_tr_pu * m.V[i] * pyo.cos(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.cos(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
            elif row["bus2_id"] == i and row["bus1_id"] in buses:
                is_empy = False
                j = row["bus1_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu_coeff = v_pu_coeff_i / v_pu_coeff_j
                ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += 1 * m.V[i] * ( - line_y_pu * ratio_tr_pu * m.V[j] * pyo.cos(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.cos(line_ksi) )
        for id_gen, row in n.get_generators().iterrows():
            if row["bus_id"] == i:
                rhs += row["target_q"] / p_pu # TO DO: to be replaced with pu getters
        for id_ld, row in n.get_loads().iterrows():
            if row["bus_id"] == i:
                rhs -= row["q0"] / p_pu # TO DO: to be replaced with pu getters
        if is_empty:
            return pyo.Constraint.Skip
        else:
            return lhs == rhs
    return reactive_power_balancePQ_expr_m

# Reactive power balance for PV buses
def reactive_power_balancePV_expr(n, buses, p_pu):
    def reactive_power_balancePV_expr_m(m,i):
        v_pu_coeff_i = n.get_voltage_levels().at[n.get_buses().at[i, "voltage_level_id"], "nominal_v"]
        lhs = 0
        rhs = 0
        is_empty = True
        for id_line, row in n.get_lines().iterrows():
            if row["bus1_id"] == i and row["bus2_id"] in buses:
                is_empy = False
                j = row["bus2_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu = 1
                line_b1_pu = row[ "b1"] / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += ratio_tr_pu * m.V[i] * (-line_b1_pu * ratio_tr_pu * m.V[i] + line_y_pu * ratio_tr_pu * m.V[i] * pyo.cos(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.cos(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
            elif row["bus2_id"] == i and row["bus1_id"] in buses:
                is_empy = False
                j = row["bus1_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu = 1
                line_b2_pu = row["b2"] / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += 1 * m.V[i] * ( - line_b2_pu * 1 * m.V[i] - line_y_pu * ratio_tr_pu * m.V[j] * pyo.cos(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.cos(line_ksi) )
        for id_tr, row in n.get_2_windings_transformers().iterrows():
            if row["bus1_id"] == i and row["bus2_id"] in buses:
                is_empy = False
                j = row["bus2_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U
                ratio_tr_pu_coeff = v_pu_coeff_j / v_pu_coeff_i
                ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += ratio_tr_pu * m.V[i] * (line_y_pu * ratio_tr_pu * m.V[i] * pyo.cos(line_ksi) - line_y_pu * 1 * m.V[j] * pyo.cos(line_ksi - 0 + 0 - m.Phi[i] + m.Phi[j]) )
            elif row["bus2_id"] == i and row["bus1_id"] in buses:
                is_empy = False
                j = row["bus1_id"]
                v_pu_coeff_j = n.get_voltage_levels().at[n.get_buses().at[j, "voltage_level_id"], "nominal_v"]
                sus_pu_coeff = p_pu / v_pu_coeff_i / v_pu_coeff_j #POWER/U/U 
                ratio_tr_pu_coeff = v_pu_coeff_i / v_pu_coeff_j
                ratio_tr_pu = row["rated_u2"] / row["rated_u1"] / ratio_tr_pu_coeff # TO DO: to be replaced with pu getters
                line_r = row["r"]
                line_x = row["x"]
                line_y_pu = 1 / np.sqrt(line_r * line_r + line_x * line_x) / sus_pu_coeff # TO DO: to be replaced with pu getters
                line_ksi = np.arctan2(line_r,line_x)
                lhs += 1 * m.V[i] * ( - line_y_pu * ratio_tr_pu * m.V[j] * pyo.cos(line_ksi + 0 - 0 + m.Phi[j] - m.Phi[i]) + line_y_pu * 1 * m.V[i] * pyo.cos(line_ksi) )
        rhs += m.Q[i] / p_pu
        if is_empty:
            return pyo.Constraint.Skip
        else:
            return lhs == rhs
    return reactive_power_balancePV_expr_m
    
# Reactive power bounds
def reactive_power_bounds(n):
    def reactive_power_bounds_m(m, i):
        lb = 0
        ub = 0
        for id_gen, row in n.get_generators().iterrows():
            if row["bus_id"] == i:
                target_p = row["target_p"]
                dist = np.nan
                for id_curve, row in n.get_reactive_capability_curve_points().iterrows():
                    if id_curve[0] == id_gen:
                        if np.isnan(dist):
                            dist = abs(target_p-row["p"])
                            minq = row["min_q"]
                            maxq = row["max_q"]
                        else:
                            if abs(target_p-row["p"]) < dist:
                                dist = abs(target_p-row["p"])
                                minq = row["min_q"]
                                maxq = row["max_q"]
                lb += minq
                ub += maxq
        for id_ld, row in n.get_loads().iterrows():
            if row["bus_id"] == i:
                lb -= row["q0"]
                ub -= row["q0"]
        return (lb, ub)
    return reactive_power_bounds_m
    
# Reactive power initialization
def reactive_init(n):
    def reactive_init_m(m,i):
        q_init = 0
        if q_init < m.Q[i].lb:
            q_init =  m.Q[i].lb
        if q_init > m.Q[i].ub:
            q_init =  m.Q[i].ub
        return q_init
    return reactive_init_m

# Voltage bounds
def voltage_bounds(n):
    def voltage_bounds_m(m, i):
        voltage_level_id = n.get_buses().at[i, "voltage_level_id"]
        v_pu_coeff = n.get_voltage_levels().at[voltage_level_id, "nominal_v"]
        v_min_pu = n.get_voltage_levels().at[voltage_level_id, "low_voltage_limit"] / v_pu_coeff # TO DO: to be replaced with pu getters
        v_max_pu = n.get_voltage_levels().at[voltage_level_id, "high_voltage_limit"] / v_pu_coeff # TO DO: to be replaced with pu getters
        return (v_min_pu, v_max_pu)
    return voltage_bounds_m

# Voltage initialization
def voltage_init(n):
    def voltage_init_m(m,i):
        voltage_level_id = n.get_buses().at[i, "voltage_level_id"]
        v_init = n.get_buses().at[i,"v_mag"]
        if np.isnan(v_init):
            v_init_pu = 1
        else:
            v_pu_coeff = n.get_voltage_levels().at[voltage_level_id, "nominal_v"]
            v_init_pu = n.get_buses().at[i,"v_mag"] / v_pu_coeff # TO DO: to be replaced with pu getters
        if v_init_pu < m.V[i].lb:
            v_init_pu =  m.V[i].lb
        if v_init_pu > m.V[i].ub:
            v_init_pu =  m.V[i].ub
        return v_init_pu
    return voltage_init_m