import axios from "axios";
import type {NetworkInfo, Sld} from "./types.ts";

export const loadNetwork = (filePath: string) =>
    axios
        .post<{ networkInfo: NetworkInfo }>('/api/network/load', { file_path: filePath })
        .then((res) => res.data.networkInfo)

export const getVoltageLevelIds = () =>
    axios
        .get<string[]>('/api/network/infos/voltage_levels')
        .then((res) => res.data)

export const getSld = (vlId: string) =>
    axios.post<Sld>('/api/network/sld', { vlId: vlId }).then((res) => res.data)
