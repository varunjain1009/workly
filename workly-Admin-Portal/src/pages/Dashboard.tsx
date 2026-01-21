import { useEffect, useState } from 'react';
import { getDashboardStats } from '../api';
import type { DashboardStats } from '../api';
import { Users, Briefcase, TrendingUp, DollarSign, Activity } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell, Tooltip, CartesianGrid, XAxis, YAxis } from 'recharts';

export default function Dashboard() {
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [loading, setLoading] = useState(true);

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

    if (loading) return <div className="p-10 text-center text-slate-500">Loading dashboard...</div>;
    // Check if we have any meaningful data
    const hasData = stats && (stats.totalUsers > 0 || stats.activeJobs > 0 || stats.revenue > 0);

    if (!stats || !hasData) return (
        <div className="p-10 text-center">
            <div className="mb-4 text-slate-300">
                <Activity size={48} className="mx-auto" />
            </div>
            <h2 className="text-xl font-semibold text-slate-700">No Analytics Data Available</h2>
            <p className="text-slate-500 mt-2">
                Once users sign up and jobs are posted, your dashboard will light up with insights.
            </p>
        </div>
    );

    const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];
    const pieData = Object.entries(stats.jobStatusDistribution).map(([name, value]) => ({ name, value }));

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold text-slate-800 mb-6">Dashboard</h1>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 mb-8">
                <StatCard
                    title="Total Users"
                    value={stats.totalUsers.toLocaleString()}
                    icon={<Users className="text-blue-600" size={24} />}
                    trend=""
                />
                <StatCard
                    title="Seekers"
                    value={stats.totalSeekers.toLocaleString()}
                    icon={<Users className="text-indigo-600" size={24} />}
                    trend=""
                />
                <StatCard
                    title="Providers"
                    value={stats.totalWorkers.toLocaleString()}
                    icon={<Briefcase className="text-orange-600" size={24} />}
                    trend=""
                />
                <StatCard
                    title="New Users Today"
                    value={stats.newUsersToday.toLocaleString()}
                    icon={<TrendingUp className="text-green-600" size={24} />}
                    trend=""
                />
                <StatCard
                    title="Active Jobs"
                    value={stats.activeJobs.toLocaleString()}
                    icon={<Activity className="text-purple-600" size={24} />}
                    trend=""
                />
                <StatCard
                    title="Revenue"
                    value={`$${stats.revenue.toLocaleString()}`}
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
                            <LineChart data={stats.userGrowth}>
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
        </div>
    );
}

function StatCard({ title, value, icon, trend }: { title: string, value: string, icon: React.ReactNode, trend: string }) {
    return (
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-sm font-medium text-slate-500">{title}</h3>
                    <p className="text-2xl font-bold text-slate-800 mt-1">{value}</p>
                </div>
                <div className="p-2 bg-slate-50 rounded-lg">
                    {icon}
                </div>
            </div>
            {trend && <p className="text-xs text-slate-400">{trend}</p>}
        </div>
    );
}
