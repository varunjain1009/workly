import { useEffect, useState } from 'react';
import { Plus, History, RotateCcw, Save } from 'lucide-react';
import { getConfigs, createConfig, getConfigHistory, rollbackConfig } from '../api';
import type { Config } from '../api';

export default function Configs() {
    const [configs, setConfigs] = useState<Config[]>([]);
    const [scope, setScope] = useState('GLOBAL');
    const [selectedConfig, setSelectedConfig] = useState<Config | null>(null);
    const [history, setHistory] = useState<Config[]>([]);
    const [showHistory, setShowHistory] = useState(false);

    // Form State
    const [isCreating, setIsCreating] = useState(false);
    const [formKey, setFormKey] = useState('');
    const [formValue, setFormValue] = useState('');

    useEffect(() => {
        loadConfigs();
    }, [scope]);

    const loadConfigs = async () => {
        const res = await getConfigs(scope);
        setConfigs(res.data);
    };

    const handleCreate = async () => {
        await createConfig(formKey, formValue, scope, 'admin_ui');
        setIsCreating(false);
        setFormKey('');
        setFormValue('');
        loadConfigs();
    };

    const handleUpdate = async (key: string, currentValue: string) => {
        // Defines "Quick Update" by setting form state
        setFormKey(key);
        setFormValue(currentValue);
        setIsCreating(true);
    };

    const loadHistory = async (key: string) => {
        const res = await getConfigHistory(key, scope);
        setHistory(res.data);
        setShowHistory(true);
    };

    const handleRollback = async (key: string, version: number) => {
        if (!confirm(`Rollback ${key} to v${version}?`)) return;
        await rollbackConfig(key, scope, version, 'admin_ui');
        setShowHistory(false);
        loadConfigs();
    };

    return (
        <div className="p-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold text-slate-800">Configurations</h1>
                <button onClick={() => setIsCreating(true)} className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
                    <Plus size={18} /> New Config
                </button>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-left">
                    <thead className="bg-slate-50 text-slate-500 text-sm uppercase">
                        <tr>
                            <th className="p-4">Key</th>
                            <th className="p-4">Value</th>
                            <th className="p-4">Scope</th>
                            <th className="p-4">Version</th>
                            <th className="p-4">Updated By</th>
                            <th className="p-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {configs.map((cfg) => (
                            <tr key={cfg.id} className="hover:bg-slate-50">
                                <td className="p-4 font-medium text-slate-700">{cfg.key}</td>
                                <td className="p-4 font-mono text-xs bg-slate-100 rounded text-slate-600">{cfg.value}</td>
                                <td className="p-4 text-slate-500">{cfg.scope}</td>
                                <td className="p-4 text-blue-600 font-bold">v{cfg.version}</td>
                                <td className="p-4 text-slate-400 text-sm">{cfg.createdBy}</td>
                                <td className="p-4 flex gap-2">
                                    <button onClick={() => handleUpdate(cfg.key, cfg.value)} className="p-2 text-slate-500 hover:text-blue-600" title="Edit">
                                        <Save size={18} />
                                    </button>
                                    <button onClick={() => { setSelectedConfig(cfg); loadHistory(cfg.key); }} className="p-2 text-slate-500 hover:text-purple-600" title="History">
                                        <History size={18} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {configs.length === 0 && <div className="p-8 text-center text-slate-400">No configurations found.</div>}
            </div>

            {/* Create/Edit Modal */}
            {isCreating && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h2 className="text-xl font-bold mb-4">Save Configuration</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">Key</label>
                                <input value={formKey} onChange={e => setFormKey(e.target.value)} className="w-full p-2 border rounded-lg" placeholder="MAX_RADIUS" />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">Value</label>
                                <textarea value={formValue} onChange={e => setFormValue(e.target.value)} className="w-full p-2 border rounded-lg h-32 font-mono text-sm" placeholder="100" />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">Scope</label>
                                <input value={scope} onChange={e => setScope(e.target.value)} className="w-full p-2 border rounded-lg" />
                            </div>
                        </div>
                        <div className="flex justify-end gap-2 mt-6">
                            <button onClick={() => setIsCreating(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg">Cancel</button>
                            <button onClick={handleCreate} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">Save</button>
                        </div>
                    </div>
                </div>
            )}

            {/* History Modal */}
            {showHistory && selectedConfig && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 max-h-[80vh] overflow-auto">
                        <h2 className="text-xl font-bold mb-4">History: {selectedConfig.key}</h2>
                        <div className="space-y-3">
                            {history.map((ver) => (
                                <div key={ver.id} className={`p-4 rounded-lg border flex justify-between items-center ${ver.active ? 'bg-green-50 border-green-200' : 'bg-slate-50 border-slate-200'}`}>
                                    <div>
                                        <div className="flex items-center gap-2">
                                            <span className="font-bold text-lg">v{ver.version}</span>
                                            {ver.active && <span className="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded-full">ACTIVE</span>}
                                        </div>
                                        <p className="font-mono text-sm text-slate-600 mt-1">{ver.value}</p>
                                        <p className="text-xs text-slate-400 mt-2">Updated by {ver.createdBy} on {ver.createdAt}</p>
                                    </div>
                                    {!ver.active && (
                                        <button onClick={() => handleRollback(ver.key, ver.version)} className="flex items-center gap-1 text-sm bg-white border border-slate-300 px-3 py-1.5 rounded-lg hover:bg-slate-50">
                                            <RotateCcw size={14} /> Rollback
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                        <div className="flex justify-end gap-2 mt-6">
                            <button onClick={() => setShowHistory(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg">Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
