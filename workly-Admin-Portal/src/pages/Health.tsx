import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, ExternalLink } from 'lucide-react';
import { getSystemHealth } from '../api';
import type { ServiceHealth, InfraHealth, SystemHealthReport } from '../api';

const REFRESH_INTERVAL_MS = 30000;

function StatusDot({ status }: { status: string }) {
    const color =
        status === 'UP' ? 'bg-green-400' :
        status === 'DOWN' ? 'bg-red-500' :
        status === 'DEGRADED' ? 'bg-yellow-400' :
        'bg-slate-400';
    return <span className={`inline-block w-2.5 h-2.5 rounded-full ${color} flex-shrink-0`} />;
}

function StatusBadge({ status }: { status: string }) {
    const cls =
        status === 'UP' ? 'bg-green-100 text-green-800' :
        status === 'DOWN' ? 'bg-red-100 text-red-700' :
        status === 'DEGRADED' ? 'bg-yellow-100 text-yellow-800' :
        'bg-slate-100 text-slate-600';
    return (
        <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-semibold ${cls}`}>
            <StatusDot status={status} />
            {status}
        </span>
    );
}

function formatBytes(bytes: number): string {
    if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(1) + ' GB';
    if (bytes >= 1048576) return (bytes / 1048576).toFixed(0) + ' MB';
    return (bytes / 1024).toFixed(0) + ' KB';
}

function MemoryBar({ used, max }: { used?: number; max?: number }) {
    if (!used || !max || max <= 0) return <span className="text-xs text-slate-400">—</span>;
    const pct = Math.min(100, Math.round((used / max) * 100));
    const color = pct > 85 ? 'bg-red-400' : pct > 65 ? 'bg-yellow-400' : 'bg-green-400';
    return (
        <div className="w-full">
            <div className="flex justify-between text-xs text-slate-500 mb-1">
                <span>{formatBytes(used)}</span>
                <span>{pct}%</span>
            </div>
            <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}

function CpuBar({ value }: { value?: number }) {
    if (value == null) return <span className="text-xs text-slate-400">—</span>;
    const pct = Math.round(value * 100);
    const color = pct > 80 ? 'bg-red-400' : pct > 50 ? 'bg-yellow-400' : 'bg-blue-400';
    return (
        <div className="w-full">
            <div className="flex justify-between text-xs text-slate-500 mb-1">
                <span>CPU</span>
                <span>{pct}%</span>
            </div>
            <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}

function ServiceCard({ svc }: { svc: ServiceHealth }) {
    return (
        <div className="bg-white rounded-xl border border-slate-200 p-4 flex flex-col gap-3">
            <div className="flex items-start justify-between gap-2">
                <div>
                    <p className="font-semibold text-slate-800 text-sm">{svc.name}</p>
                    <p className="text-xs text-slate-400">:{svc.port}</p>
                </div>
                <StatusBadge status={svc.status} />
            </div>

            {svc.status !== 'DOWN' && (
                <>
                    <CpuBar value={svc.cpuUsage} />
                    <MemoryBar used={svc.memoryUsedBytes} max={svc.memoryMaxBytes} />
                </>
            )}

            {svc.error && (
                <p className="text-xs text-red-500 truncate" title={svc.error}>{svc.error}</p>
            )}

            {svc.components && Object.keys(svc.components).length > 0 && (
                <div className="flex flex-wrap gap-1.5 pt-1 border-t border-slate-100">
                    {Object.entries(svc.components).map(([k, v]) => (
                        <span key={k} className="flex items-center gap-1 text-xs text-slate-600 bg-slate-50 px-2 py-0.5 rounded">
                            <StatusDot status={v} />
                            {k}
                        </span>
                    ))}
                </div>
            )}
        </div>
    );
}

const CATEGORY_LABELS: Record<string, string> = {
    DATABASE: 'Databases',
    CACHE: 'Cache',
    MESSAGING: 'Messaging',
    SEARCH: 'Search',
    OBSERVABILITY: 'Observability',
};

const OBSERVABILITY_LINKS: Record<string, { port: number; path: string }> = {
    Prometheus: { port: 9090, path: '' },
    Grafana: { port: 3000, path: '' },
    Jaeger: { port: 16686, path: '' },
};

function InfraGroup({ category, nodes }: { category: string; nodes: InfraHealth[] }) {
    return (
        <div>
            <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                {CATEGORY_LABELS[category] ?? category}
            </h3>
            <div className="space-y-1.5">
                {nodes.map((n) => {
                    const obsLink = OBSERVABILITY_LINKS[n.name];
                    return (
                        <div key={n.name + n.port}
                            className="flex items-center justify-between bg-white rounded-lg border border-slate-200 px-3 py-2">
                            <div className="flex items-center gap-2 min-w-0">
                                <StatusDot status={n.status} />
                                <span className="text-sm font-medium text-slate-700 truncate">{n.name}</span>
                                <span className="text-xs text-slate-400 flex-shrink-0">{n.host}:{n.port}</span>
                            </div>
                            <div className="flex items-center gap-2 flex-shrink-0">
                                <StatusBadge status={n.status} />
                                {obsLink && n.status === 'UP' && (
                                    <a
                                        href={`http://localhost:${obsLink.port}${obsLink.path}`}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="text-blue-500 hover:text-blue-700"
                                        title={`Open ${n.name}`}
                                    >
                                        <ExternalLink size={14} />
                                    </a>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

function groupBy<T>(arr: T[], key: (item: T) => string): Record<string, T[]> {
    return arr.reduce((acc, item) => {
        const k = key(item);
        (acc[k] = acc[k] || []).push(item);
        return acc;
    }, {} as Record<string, T[]>);
}

const CATEGORY_ORDER = ['DATABASE', 'CACHE', 'MESSAGING', 'SEARCH', 'OBSERVABILITY'];

export default function Health() {
    const [report, setReport] = useState<SystemHealthReport | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [lastRefresh, setLastRefresh] = useState<Date | null>(null);

    const fetchHealth = useCallback(async () => {
        try {
            const resp = await getSystemHealth();
            setReport(resp.data);
            setLastRefresh(new Date());
            setError(null);
        } catch (e: any) {
            setError(e?.response?.data?.message ?? e?.message ?? 'Failed to fetch health data');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchHealth();
        const interval = setInterval(fetchHealth, REFRESH_INTERVAL_MS);
        return () => clearInterval(interval);
    }, [fetchHealth]);

    const infraByCategory = report
        ? groupBy(report.infrastructure, (n) => n.category)
        : {};

    const upServices = report?.services.filter((s) => s.status === 'UP').length ?? 0;
    const totalServices = report?.services.length ?? 0;
    const upInfra = report?.infrastructure.filter((n) => n.status === 'UP').length ?? 0;
    const totalInfra = report?.infrastructure.length ?? 0;

    return (
        <div className="p-6 space-y-6 max-w-7xl mx-auto">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">System Health</h1>
                    {lastRefresh && (
                        <p className="text-xs text-slate-400 mt-0.5">
                            Last updated {lastRefresh.toLocaleTimeString()} · auto-refreshes every 30s
                        </p>
                    )}
                </div>
                <button
                    onClick={() => { setLoading(true); fetchHealth(); }}
                    disabled={loading}
                    className="flex items-center gap-2 px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-50"
                >
                    <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
                    Refresh
                </button>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
                    {error}
                </div>
            )}

            {/* Summary pills */}
            {report && (
                <div className="flex gap-3">
                    <div className="bg-white border border-slate-200 rounded-lg px-4 py-2 text-sm">
                        <span className="text-slate-500">Services </span>
                        <span className={`font-bold ${upServices === totalServices ? 'text-green-600' : 'text-red-600'}`}>
                            {upServices}/{totalServices} UP
                        </span>
                    </div>
                    <div className="bg-white border border-slate-200 rounded-lg px-4 py-2 text-sm">
                        <span className="text-slate-500">Infrastructure </span>
                        <span className={`font-bold ${upInfra === totalInfra ? 'text-green-600' : 'text-red-600'}`}>
                            {upInfra}/{totalInfra} UP
                        </span>
                    </div>
                </div>
            )}

            {loading && !report && (
                <div className="flex items-center justify-center h-48 text-slate-400">
                    <RefreshCw size={20} className="animate-spin mr-2" />
                    Probing services…
                </div>
            )}

            {report && (
                <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                    {/* Microservices — wider column */}
                    <div className="xl:col-span-2 space-y-3">
                        <h2 className="text-base font-semibold text-slate-700">Microservices</h2>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {report.services.map((svc) => (
                                <ServiceCard key={svc.name} svc={svc} />
                            ))}
                        </div>
                    </div>

                    {/* Infrastructure — right column */}
                    <div className="space-y-5">
                        <h2 className="text-base font-semibold text-slate-700">Infrastructure</h2>
                        {CATEGORY_ORDER.filter((c) => infraByCategory[c]).map((cat) => (
                            <InfraGroup key={cat} category={cat} nodes={infraByCategory[cat]} />
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
