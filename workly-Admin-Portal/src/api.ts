import axios from 'axios';

const api = axios.create({
    baseURL: '/api/v1',
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

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

export interface Skill {
    id: string;
    canonicalName: string;
    aliases: string[];
    phonetic: string;
    status: string;
}

export const getSkills = async () => {
    return api.get<Skill[]>('/skills');
}

export const addAliases = async (skillName: string, aliases: string[]) => {
    return api.post(`/skills/${skillName}/aliases`, aliases);
}

export interface DashboardStats {
    totalUsers: number;
    totalSeekers: number;
    totalWorkers: number;
    newUsersToday: number;
    activeJobs: number;
    completedJobs: number;
    revenue: number;
    userGrowth: { label: string; value: number }[];
    jobStatusDistribution: Record<string, number>;
}

export const getDashboardStats = async () => {
    return api.get<DashboardStats>('/analytics/dashboard');
}

export interface QueryRequest {
    type: 'SQL' | 'MONGO';
    query: string;
    collection?: string;
}

export const executeReport = async (req: QueryRequest) => {
    return api.post<any[]>('/reports/execute', req);
}

export const getSqlSchema = async () => {
    return api.get<Record<string, string[]>>('/reports/schema/sql');
}

export const getMongoCollections = async () => {
    return api.get<string[]>('/reports/schema/mongo');
}

export const getMongoSample = async (collection: string) => {
    return api.get<any>(`/reports/schema/mongo/${collection}/sample`);
}
