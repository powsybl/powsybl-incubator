import axios from "axios";
import type {NetworkInfo, Sld} from "./types.ts";
import type {ChangeSetEntry} from "@powsybl/network-editor";

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


export const deleteElement = (elementId: string) =>
    axios.delete(`/api/network/elements/${String(elementId)}`).then((res) => res.data)

export const deleteBay = (elementId: string) =>
    axios.delete(`/api/network/bay/${String(elementId)}`).then((res) => res.data)

export const applyChange = (change: ChangeSetEntry) =>
    axios.post('/api/network/changes', change).then((res) => res.data)
