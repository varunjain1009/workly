import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Settings, Activity, LayoutDashboard, Database, BarChart2 } from 'lucide-react';
import { useState } from 'react';

import Configs from './pages/Configs';
import AuditLogs from './pages/AuditLogs';
import Skills from './pages/Skills';
import Dashboard from './pages/Dashboard';
import CustomReports from './pages/CustomReports';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <Router>
      <div className="flex h-screen bg-gray-100">
        {/* Sidebar */}
        <aside className="w-64 bg-slate-900 text-white flex flex-col">
          <div className="p-6 border-b border-slate-700">
            <h1 className="text-xl font-bold flex items-center gap-2">
              <Database className="text-blue-400" />
              Workly Admin
            </h1>
          </div>
          <nav className="flex-1 p-4 space-y-2">
            <Link to="/" onClick={() => setActiveTab('dashboard')}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${activeTab === 'dashboard' ? 'bg-blue-600' : 'hover:bg-slate-800'}`}>
              <LayoutDashboard size={20} />
              Dashboard
            </Link>
            <Link to="/skills" onClick={() => setActiveTab('skills')}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${activeTab === 'skills' ? 'bg-blue-600' : 'hover:bg-slate-800'}`}>
              <Database size={20} />
              Expertise
            </Link>
            <Link to="/configs" onClick={() => setActiveTab('configs')}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${activeTab === 'configs' ? 'bg-blue-600' : 'hover:bg-slate-800'}`}>
              <Settings size={20} />
              Configurations
            </Link>
            <Link to="/audit" onClick={() => setActiveTab('audit')}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${activeTab === 'audit' ? 'bg-blue-600' : 'hover:bg-slate-800'}`}>
              <Activity size={20} />
              Audit Logs
            </Link>
            <Link to="/reports" onClick={() => setActiveTab('reports')}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${activeTab === 'reports' ? 'bg-blue-600' : 'hover:bg-slate-800'}`}>
              <BarChart2 size={20} />
              Reports
            </Link>
          </nav>
          <div className="p-4 border-t border-slate-700">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center font-bold">A</div>
              <div>
                <p className="text-sm font-medium">Admin User</p>
                <p className="text-xs text-slate-400">Super Admin</p>
              </div>
            </div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="flex-1 overflow-auto">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/skills" element={<Skills />} />
            <Route path="/configs" element={<Configs />} />
            <Route path="/audit" element={<AuditLogs />} />
            <Route path="/reports" element={<CustomReports />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
