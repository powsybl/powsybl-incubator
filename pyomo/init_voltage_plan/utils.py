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