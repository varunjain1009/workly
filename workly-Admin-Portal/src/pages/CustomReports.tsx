import { useState, useEffect } from 'react';
import { executeReport, type QueryRequest, getSqlSchema, getMongoCollections, getMongoSample } from '../api';
import { Play, Activity, Database, FileJson, BarChart2 } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, LineChart, Line } from 'recharts';

export default function CustomReports() {
    const [mode, setMode] = useState<'SQL' | 'MONGO'>('SQL');
    const [query, setQuery] = useState('');
    const [collection, setCollection] = useState('');
    const [results, setResults] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedTable, setSelectedTable] = useState('');
    const [selectedCollection, setSelectedCollection] = useState('');

    // Metadata State
    const [sqlSchema, setSqlSchema] = useState<Record<string, string[]>>({});
    const [mongoCollections, setMongoCollections] = useState<string[]>([]);
    const [sampleDoc, setSampleDoc] = useState<any>(null);

    // Fetch Metadata
    useEffect(() => {
        if (mode === 'SQL') {
            getSqlSchema().then(res => setSqlSchema(res.data)).catch(console.error);
        } else {
            getMongoCollections().then(res => setMongoCollections(res.data)).catch(console.error);
        }
    }, [mode]);

    const handleCollectionSelect = async (e: React.ChangeEvent<HTMLSelectElement>) => {
        const col = e.target.value;
        setSelectedCollection(col);
        setCollection(col); // Auto-fill the input if needed, or we might replace the input entirely
        if (col) {
            try {
                const res = await getMongoSample(col);
                setSampleDoc(res.data);
            } catch (e) {
                console.error(e);
                setSampleDoc(null);
            }
        } else {
            setSampleDoc(null);
        }
    };

    // Chart Configuration
    const [xAxis, setXAxis] = useState('');
    const [yAxis, setYAxis] = useState('');
    const [chartType, setChartType] = useState<'BAR' | 'LINE'>('BAR');
    const [error, setError] = useState<string | null>(null);


    const handleRun = async () => {
        setLoading(true);
        setError(null);
        setResults([]);
        try {
            const req: QueryRequest = { type: mode, query };
            if (mode === 'MONGO') req.collection = collection;

            const res = await executeReport(req);
            setResults(res.data);

            // Auto-detect keys for chart config if data exists
            if (res.data.length > 0) {
                const keys = Object.keys(res.data[0]);
                if (keys.length >= 2) {
                    setXAxis(keys[0]);
                    setYAxis(keys[1]);
                }
            }
        } catch (e: any) {
            setError(e.response?.data?.message || e.message || "Query failed");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="p-6 max-w-7xl mx-auto">
            <h1 className="text-2xl font-bold text-slate-800 mb-6 flex items-center gap-2">
                <BarChart2 className="text-blue-600" />
                Custom Reports Builder
            </h1>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Editor Column */}
                <div className="lg:col-span-1 space-y-6">
                    <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                        <div className="flex gap-2 mb-6 bg-slate-100 p-1 rounded-lg">
                            <button
                                onClick={() => setMode('SQL')}
                                className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-md text-sm font-medium transition-all ${mode === 'SQL' ? 'bg-white shadow text-blue-600' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                <Database size={16} /> SQL
                            </button>
                            <button
                                onClick={() => setMode('MONGO')}
                                className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-md text-sm font-medium transition-all ${mode === 'MONGO' ? 'bg-white shadow text-green-600' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                <FileJson size={16} /> MongoDB
                            </button>
                        </div>

                        {/* Schema Explorer */}
                        <div className="mb-6 p-4 bg-slate-50 rounded-lg border border-slate-200">
                            <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">
                                {mode === 'SQL' ? 'Explore Tables' : 'Explore Collections'}
                            </label>

                            {mode === 'SQL' ? (
                                <>
                                    <select
                                        value={selectedTable}
                                        onChange={(e) => setSelectedTable(e.target.value)}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm mb-3 focus:ring-2 focus:ring-blue-500 outline-none"
                                        disabled={Object.keys(sqlSchema).length === 0}
                                    >
                                        <option value="">{Object.keys(sqlSchema).length === 0 ? "No tables found" : "Select a table..."}</option>
                                        {Object.keys(sqlSchema).map(t => <option key={t} value={t}>{t}</option>)}
                                    </select>

                                    {selectedTable && sqlSchema[selectedTable] && (
                                        <div className="flex flex-wrap gap-1.5 max-h-32 overflow-y-auto">
                                            {sqlSchema[selectedTable].map(col => (
                                                <span key={col} className="px-2 py-1 bg-white border border-slate-200 rounded text-xs text-slate-600 font-mono">
                                                    {col}
                                                </span>
                                            ))}
                                        </div>
                                    )}
                                </>
                            ) : (
                                <>
                                    <select
                                        value={selectedCollection}
                                        onChange={handleCollectionSelect}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm mb-3 focus:ring-2 focus:ring-blue-500 outline-none"
                                    >
                                        <option value="">Select a collection...</option>
                                        {mongoCollections.map(c => <option key={c} value={c}>{c}</option>)}
                                    </select>

                                    {selectedCollection && (
                                        <div className="bg-white border border-slate-200 rounded p-2 overflow-x-auto max-h-40">
                                            {sampleDoc ? (
                                                <pre className="text-xs text-slate-600 font-mono">
                                                    {JSON.stringify(sampleDoc, null, 2)}
                                                </pre>
                                            ) : (
                                                <div className="text-xs text-slate-400 italic text-center py-2">
                                                    No data present in this collection
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </>
                            )}
                        </div>

                        {mode === 'MONGO' && (
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-slate-700 mb-1">Target Collection</label>
                                <input
                                    type="text"
                                    value={collection}
                                    onChange={(e) => setCollection(e.target.value)}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                                    placeholder="e.g., job"
                                />
                            </div>
                        )}

                        <div className="mb-4">
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                {mode === 'SQL' ? 'Query (Read-Only)' : 'Query (JSON)'}
                            </label>
                            <textarea
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                className="w-full h-48 px-3 py-2 font-mono text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all resize-y"
                                placeholder={mode === 'SQL' ? "SELECT * FROM users" : "{ \"status\": \"COMPLETED\" }"}
                            />
                        </div>

                        <button
                            onClick={handleRun}
                            disabled={loading || !query}
                            className="w-full flex items-center justify-center gap-2 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? <Activity className="animate-spin" size={18} /> : <Play size={18} />}
                            Running Query...
                        </button>

                        {error && (
                            <div className="mt-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg border border-red-200">
                                <strong>Error:</strong> {error}
                            </div>
                        )}
                    </div>

                    {/* Chart Config */}
                    {results.length > 0 && (
                        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                            <h3 className="font-semibold text-slate-800 mb-4">Visual Configuration</h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-xs font-medium text-slate-500 uppercase mb-1">X Axis (Category)</label>
                                    <select
                                        value={xAxis}
                                        onChange={(e) => setXAxis(e.target.value)}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                                    >
                                        {Object.keys(results[0]).map(key => <option key={key} value={key}>{key}</option>)}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-xs font-medium text-slate-500 uppercase mb-1">Y Axis (Value)</label>
                                    <select
                                        value={yAxis}
                                        onChange={(e) => setYAxis(e.target.value)}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                                    >
                                        {Object.keys(results[0]).map(key => <option key={key} value={key}>{key}</option>)}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-xs font-medium text-slate-500 uppercase mb-1">Chart Type</label>
                                    <div className="flex bg-slate-100 p-1 rounded-lg">
                                        <button
                                            onClick={() => setChartType('BAR')}
                                            className={`flex-1 text-xs py-1.5 rounded ${chartType === 'BAR' ? 'bg-white shadow' : ''}`}
                                        >
                                            Bar
                                        </button>
                                        <button
                                            onClick={() => setChartType('LINE')}
                                            className={`flex-1 text-xs py-1.5 rounded ${chartType === 'LINE' ? 'bg-white shadow' : ''}`}
                                        >
                                            Line
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Results Column */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Visualization Area */}
                    {results.length > 0 && (
                        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm h-80">
                            <ResponsiveContainer width="100%" height="100%">
                                {chartType === 'BAR' ? (
                                    <BarChart data={results}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                        <XAxis dataKey={xAxis} />
                                        <YAxis />
                                        <Tooltip />
                                        <Legend />
                                        <Bar dataKey={yAxis} fill="#3B82F6" radius={[4, 4, 0, 0]} />
                                    </BarChart>
                                ) : (
                                    <LineChart data={results}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                        <XAxis dataKey={xAxis} />
                                        <YAxis />
                                        <Tooltip />
                                        <Legend />
                                        <Line type="monotone" dataKey={yAxis} stroke="#3B82F6" strokeWidth={3} dot={{ r: 4 }} />
                                    </LineChart>
                                )}
                            </ResponsiveContainer>
                        </div>
                    )}

                    {/* Raw Data Table */}
                    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                        <div className="p-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                            <h3 className="font-semibold text-slate-800">Raw Results</h3>
                            <span className="text-xs text-slate-500">{results.length} rows</span>
                        </div>
                        <div className="overflow-x-auto max-h-96">
                            <table className="w-full text-sm text-left">
                                <thead className="text-xs text-slate-500 uppercase bg-slate-50 sticky top-0">
                                    <tr>
                                        {results.length > 0 ? (
                                            Object.keys(results[0]).map(key => (
                                                <th key={key} className="px-6 py-3 font-medium whitespace-nowrap">{key}</th>
                                            ))
                                        ) : (
                                            <th className="px-6 py-3">No Data</th>
                                        )}
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {results.map((row, i) => (
                                        <tr key={i} className="hover:bg-slate-50">
                                            {Object.values(row).map((val: any, j) => (
                                                <td key={j} className="px-6 py-3 whitespace-nowrap text-slate-600">
                                                    {typeof val === 'object' ? JSON.stringify(val) : String(val)}
                                                </td>
                                            ))}
                                        </tr>
                                    ))}
                                    {results.length === 0 && !loading && (
                                        <tr>
                                            <td className="px-6 py-8 text-center text-slate-400">
                                                Execute a query to see results here
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
