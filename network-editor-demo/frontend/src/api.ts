import axios from "axios";
import type {NetworkInfo, Sld} from "./types.ts";
import type {ChangeSet} from "@powsybl/network-editor";

export const loadNetwork = (filePath: string) =>
    axios
        .post<{ networkInfo: NetworkInfo }>('/api/network/load', { file_path: filePath })
        .then((res) => res.data.networkInfo)

export const uploadNetwork = (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return axios
        .post<{ networkInfo: NetworkInfo }>('/api/network/upload', formData)
        .then((res) => res.data.networkInfo)
}

export const getVoltageLevelIds = () =>
    axios
        .get<string[]>('/api/network/infos/voltage_levels')
        .then((res) => res.data)

export const getSld = (vlId: string) =>
    axios.post<Sld>('/api/network/sld', { vlId: vlId }).then((res) => res.data)


export const applyModifications = (changes: ChangeSet) =>
    axios.post<void>('/api/network/modifications', changes).then((res) => res.data)
