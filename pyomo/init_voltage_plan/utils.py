# Count neighbours function
def nb_neighboursPV(net, PV_buses):
    neighbours_count = dict()
    buses = net.get_buses().index.tolist()
    for line in net.get_lines().index.tolist():
        b1 = net.get_lines().at[line, "bus1_id"]
        b2 = net.get_lines().at[line, "bus2_id"]
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
    for tr in net.get_2_windings_transformers().index.tolist():
        b1 = net.get_2_windings_transformers().at[tr, "bus1_id"]
        b2 = net.get_2_windings_transformers().at[tr, "bus2_id"]
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