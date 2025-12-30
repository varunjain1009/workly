import axios from 'axios';

const api = axios.create({
    baseURL: '/api/v1',
});

export interface Config {
    id: string;
    key: string;
    value: string;
    scope: string;
    version: number;
    active: boolean;
    model: string;
    createdBy: string;
    createdAt: string;
}

export const getConfigs = async (scope = 'GLOBAL') => {
    return api.get<Config[]>(`/configs?scope=${scope}`);
};

export const getConfigHistory = async (key: string, scope: string) => {
    return api.get<Config[]>(`/configs/${key}/history?scope=${scope}`);
}

export const createConfig = async (key: string, value: string, scope: string, adminId: string) => {
    const params = new URLSearchParams();
    params.append('key', key);
    params.append('value', value);
    params.append('scope', scope);
    params.append('adminId', adminId);
    return api.post('/configs', params);
};

export const getConfig = async (key: string, scope: string) => {
    return api.get<Config>(`/configs/${key}?scope=${scope}`);
}

export const rollbackConfig = async (key: string, scope: string, version: number, adminId: string) => {
    const params = new URLSearchParams();
    params.append('scope', scope);
    params.append('version', version.toString());
    params.append('adminId', adminId);
    return api.post(`/configs/${key}/rollback`, params);
}
