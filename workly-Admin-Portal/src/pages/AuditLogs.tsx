import { useEffect, useState } from 'react';
import { getConfigs } from '../api';
import type { Config } from '../api';
import { Clock } from 'lucide-react';

export default function AuditLogs() {
    const [logs, setLogs] = useState<Config[]>([]);

    useEffect(() => {
        loadLogs();
    }, []);

    const loadLogs = async () => {
        // For MVP, we fetch all active configs. 
        // Ideally we should fetch the actual history stream /audit-logs
        const res = await getConfigs('GLOBAL');
        // Mock sorting by "recent" since we don't have a dedicated audit stream endpoint yet
        setLogs(res.data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
    };

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold text-slate-800 mb-6 flex items-center gap-2">
                <Clock /> Recent Changes
            </h1>
            <div className="bg-white rounded-xl shadow-sm border border-slate-200">
                <div className="divide-y divide-slate-100">
                    {logs.map((log) => (
                        <div key={log.id} className="p-4 hover:bg-slate-50 flex justify-between items-center">
                            <div>
                                <p className="font-bold text-slate-700">{log.key}</p>
                                <p className="text-sm text-slate-500">Updated to <span className="font-mono bg-slate-100 px-1 rounded">{log.value}</span></p>
                            </div>
                            <div className="text-right">
                                <span className="text-xs font-bold bg-blue-100 text-blue-700 px-2 py-1 rounded-full">v{log.version}</span>
                                <p className="text-xs text-slate-400 mt-1">{new Date(log.createdAt).toLocaleString()} by {log.createdBy}</p>
                            </div>
                        </div>
                    ))}
                    {logs.length === 0 && <div className="p-8 text-center text-slate-400">No logs found.</div>}
                </div>
            </div>
        </div>
    );
}
