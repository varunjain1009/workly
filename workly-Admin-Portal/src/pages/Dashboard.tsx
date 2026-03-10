import { useEffect, useState } from 'react';
import { getDashboardStats, getSeekers, getProviders, getJobs, type PaginatedResponse } from '../api';
import type { DashboardStats } from '../api';
import { Users, Briefcase, TrendingUp, DollarSign, Activity, X, ChevronLeft, ChevronRight, Database } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell, Tooltip, CartesianGrid, XAxis, YAxis } from 'recharts';

type ModalType = 'seekers' | 'providers' | 'jobs' | null;

export default function Dashboard() {
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [loading, setLoading] = useState(true);

    // Modal State
    const [modalType, setModalType] = useState<ModalType>(null);
    const [modalData, setModalData] = useState<PaginatedResponse<any> | null>(null);
    const [modalLoading, setModalLoading] = useState(false);
    const [modalError, setModalError] = useState<string | null>(null);
    const [currentPage, setCurrentPage] = useState(0);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const res = await getDashboardStats();
            setStats(res.data);
        } catch (e) {
            console.error("Failed to load dashboard data", e);
        } finally {
            setLoading(false);
        }
    };

    const openModal = (type: ModalType) => {
        setModalType(type);
        setCurrentPage(0);
        fetchModalData(type, 0);
    };

    const fetchModalData = async (type: ModalType, page: number) => {
        setModalLoading(true);
        setModalError(null);
        try {
            let res;
            if (type === 'seekers') {
                res = await getSeekers(page, 10);
            } else if (type === 'providers') {
                res = await getProviders(page, 10);
            } else if (type === 'jobs') {
                res = await getJobs(page, 10);
            }
            
            // Backend returns ApiResponse<T> where the actual paginated data is inside the 'data' property
            if (res && res.data) {
                // If the response is wrapped in ApiResponse (has success and data properties)
                if ('success' in res.data && 'data' in res.data) {
                    setModalData((res.data as any).data);
                } else {
                    // Fallback in case the wrapper is not used
                    setModalData(res.data as any);
                }
            }
        } catch (e: any) {
            console.error("Failed to load drilldown data", e);
            setModalError(e.message || "Failed to load data. Is the backend server running?");
        } finally {
            setModalLoading(false);
        }
    };

    const handlePageChange = (newPage: number) => {
        setCurrentPage(newPage);
        fetchModalData(modalType, newPage);
    };

    if (loading) return <div className="p-10 text-center text-slate-500">Loading dashboard...</div>;
    const currentStats: DashboardStats = {
        totalUsers: stats?.totalUsers || 0,
        totalSeekers: stats?.totalSeekers || 0,
        totalWorkers: stats?.totalWorkers || 0,
        newUsersToday: stats?.newUsersToday || 0,
        activeJobs: stats?.activeJobs || 0,
        completedJobs: stats?.completedJobs || 0,
        revenue: stats?.revenue || 0,
        userGrowth: stats?.userGrowth || [],
        jobStatusDistribution: stats?.jobStatusDistribution || {}
    };

    const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];
    const pieData = Object.entries(currentStats.jobStatusDistribution).map(([name, value]) => ({ name, value }));

    const renderModalTable = () => {
        if (modalError) return <div className="p-4 text-center text-red-500 font-medium">{modalError}</div>;
        if (!modalData || !modalData.content || modalData.content.length === 0) return <div className="p-4 text-center text-slate-500">No data found.</div>;
        
        const keys = Object.keys(modalData.content[0]).filter(k => !['skills', 'jobHistory', 'location'].includes(k)); // Hide complex nested objects for tabular view

        return (
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500 uppercase text-xs">
                        <tr>
                            {keys.map(k => <th key={k} className="p-3">{k}</th>)}
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {modalData.content.map((row, i) => (
                            <tr key={i} className="hover:bg-slate-50">
                                {keys.map(k => (
                                    <td key={k} className="p-3 text-slate-600 truncate max-w-[150px]">
                                        {typeof row[k] === 'object' ? JSON.stringify(row[k]) : String(row[k] ?? '-')}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        );
    };

    return (
        <div className="p-6 relative">
            <h1 className="text-2xl font-bold text-slate-800 mb-6">Dashboard</h1>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 mb-8">
                <StatCard
                    title="Total Users"
                    value={currentStats.totalUsers.toLocaleString()}
                    icon={<Users className="text-blue-600" size={24} />}
                    trend="Seekers + Providers"
                />
                <StatCard
                    title="Seekers"
                    value={currentStats.totalSeekers.toLocaleString()}
                    icon={<Users className="text-indigo-600" size={24} />}
                    trend="Click to view list"
                    onClick={() => openModal('seekers')}
                />
                <StatCard
                    title="Providers"
                    value={currentStats.totalWorkers.toLocaleString()}
                    icon={<Briefcase className="text-orange-600" size={24} />}
                    trend="Click to view list"
                    onClick={() => openModal('providers')}
                />
                <StatCard
                    title="New Users Today"
                    value={currentStats.newUsersToday.toLocaleString()}
                    icon={<TrendingUp className="text-green-600" size={24} />}
                    trend="New Seekers & Providers"
                />
                <StatCard
                    title="Active Jobs"
                    value={currentStats.activeJobs.toLocaleString()}
                    icon={<Activity className="text-purple-600" size={24} />}
                    trend="Click to view list"
                    onClick={() => openModal('jobs')}
                />
                <StatCard
                    title="Revenue"
                    value={`$${currentStats.revenue.toLocaleString()}`}
                    icon={<DollarSign className="text-emerald-600" size={24} />}
                    trend=""
                />
            </div>

            {/* Charts Row 1 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                    <h3 className="font-bold text-slate-700 mb-4 flex items-center gap-2">
                        <Users size={18} className="text-slate-400" />
                        User Growth (Weekly)
                    </h3>
                    <div className="h-64">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={currentStats.userGrowth}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="label" axisLine={false} tickLine={false} />
                                <YAxis axisLine={false} tickLine={false} />
                                <Tooltip />
                                <Line type="monotone" dataKey="value" stroke="#3B82F6" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                    <h3 className="font-bold text-slate-700 mb-4 flex items-center gap-2">
                        <Briefcase size={18} className="text-slate-400" />
                        Job Status Distribution
                    </h3>
                    <div className="h-64 flex items-center justify-center">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={pieData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={80}
                                    paddingAngle={5}
                                    dataKey="value"
                                >
                                    {pieData.map((_, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip />
                            </PieChart>
                        </ResponsiveContainer>
                        <div className="ml-4 space-y-2">
                            {pieData.map((entry, index) => (
                                <div key={entry.name} className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                                    <span className="text-sm text-slate-600">{entry.name}: {entry.value}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Drilldown Modal */}
            {modalType && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-5xl max-h-[85vh] flex flex-col">
                        <div className="p-4 border-b flex justify-between items-center bg-slate-50 rounded-t-xl">
                            <h2 className="text-xl font-bold flex items-center gap-2 capitalize">
                                <Database className="text-blue-500" size={20} />
                                {modalType} List
                            </h2>
                            <button onClick={() => setModalType(null)} className="p-2 text-slate-400 hover:text-slate-700 rounded-full hover:bg-slate-200 transition-colors">
                                <X size={20} />
                            </button>
                        </div>
                        
                        <div className="p-4 flex-1 overflow-auto relative">
                            {modalLoading ? (
                                <div className="absolute inset-0 flex items-center justify-center bg-white/80 z-10">
                                    <Activity className="animate-spin text-blue-500" size={32} />
                                </div>
                            ) : null}
                            {renderModalTable()}
                        </div>

                        {modalData && (
                            <div className="p-4 border-t bg-slate-50 rounded-b-xl flex justify-between items-center">
                                <span className="text-sm text-slate-500">
                                    Showing page {modalData.pageable.pageNumber + 1} of {modalData.totalPages || 1} 
                                    <span className="mx-2">•</span> 
                                    Total: {modalData.totalElements} records
                                </span>
                                <div className="flex gap-2">
                                    <button 
                                        onClick={() => handlePageChange(currentPage - 1)} 
                                        disabled={currentPage === 0}
                                        className="p-2 border rounded-lg hover:bg-white disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                                    >
                                        <ChevronLeft size={16} /> Prev
                                    </button>
                                    <button 
                                        onClick={() => handlePageChange(currentPage + 1)} 
                                        disabled={modalData.last}
                                        className="p-2 border rounded-lg hover:bg-white disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                                    >
                                        Next <ChevronRight size={16} />
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

function StatCard({ title, value, icon, trend, onClick }: { title: string, value: string, icon: React.ReactNode, trend: string, onClick?: () => void }) {
    return (
        <div 
            onClick={onClick}
            className={`bg-white p-6 rounded-xl border border-slate-200 shadow-sm ${onClick ? 'cursor-pointer hover:border-blue-300 hover:shadow-md transition-all group' : ''}`}
        >
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-sm font-medium text-slate-500 group-hover:text-blue-600 transition-colors">{title}</h3>
                    <p className="text-2xl font-bold text-slate-800 mt-1">{value}</p>
                </div>
                <div className="p-2 bg-slate-50 rounded-lg group-hover:bg-blue-50 transition-colors">
                    {icon}
                </div>
            </div>
            {trend && <p className={`text-xs ${onClick ? 'text-blue-500 group-hover:underline' : 'text-slate-400'}`}>{trend}</p>}
        </div>
    );
}
