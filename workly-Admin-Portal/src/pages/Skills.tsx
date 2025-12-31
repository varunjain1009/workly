import { useState, useEffect } from 'react';
import { getSkills, addAliases } from '../api';
import type { Skill } from '../api';
import { Search, Plus, Save, X } from 'lucide-react';

const Skills = () => {
    const [skills, setSkills] = useState<Skill[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    // Alias Modal State
    const [selectedSkill, setSelectedSkill] = useState<Skill | null>(null);
    const [newAlias, setNewAlias] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);

    useEffect(() => {
        loadSkills();
    }, []);

    const loadSkills = async () => {
        try {
            setLoading(true);
            const response = await getSkills();
            setSkills(response.data);
        } catch (error) {
            console.error("Failed to load skills", error);
        } finally {
            setLoading(false);
        }
    };

    const handleAddAlias = async () => {
        if (!selectedSkill || !newAlias.trim()) return;

        try {
            // Split by comma if user enters multiple
            const aliasesToAdd = newAlias.split(',').map(s => s.trim()).filter(s => s.length > 0);

            await addAliases(selectedSkill.canonicalName, aliasesToAdd);

            // Refresh
            await loadSkills();
            closeModal();
        } catch (error) {
            console.error("Failed to add alias", error);
            alert("Failed to add alias");
        }
    };

    const openModal = (skill: Skill) => {
        setSelectedSkill(skill);
        setNewAlias('');
        setIsModalOpen(true);
    }

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedSkill(null);
    }

    const filteredSkills = skills.filter(skill =>
        skill.canonicalName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        skill.aliases.some(alias => alias.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    return (
        <div className="p-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Expertise / Skills</h1>
                    <p className="text-slate-500">Manage synonyms and aliases for skills.</p>
                </div>
                <div className="bg-white p-2 rounded-lg border flex items-center gap-2">
                    <Search className="text-slate-400" size={20} />
                    <input
                        type="text"
                        placeholder="Search skills..."
                        className="outline-none text-sm w-64"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            {loading ? (
                <div className="text-center py-10">Loading...</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {filteredSkills.map(skill => (
                        <div key={skill.id} className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
                            <div className="flex justify-between items-start mb-3">
                                <div>
                                    <h3 className="font-bold text-lg text-slate-800">{skill.canonicalName}</h3>
                                    <span className="text-xs font-mono text-slate-400">{skill.id}</span>
                                </div>
                                <button
                                    onClick={() => openModal(skill)}
                                    className="p-2 text-blue-600 hover:bg-blue-50 rounded-full transition-colors"
                                    title="Add Alias"
                                >
                                    <Plus size={20} />
                                </button>
                            </div>

                            <div className="space-y-2">
                                <h4 className="text-xs font-semibold text-slate-500 uppercase">Aliases</h4>
                                <div className="flex flex-wrap gap-2">
                                    {skill.aliases && skill.aliases.length > 0 ? (
                                        skill.aliases.map((alias, idx) => (
                                            <span key={idx} className="bg-slate-100 text-slate-600 px-2 py-1 rounded text-sm">
                                                {alias}
                                            </span>
                                        ))
                                    ) : (
                                        <span className="text-sm text-slate-400 italic">No aliases defined</span>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Add Alias Modal */}
            {isModalOpen && selectedSkill && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden">
                        <div className="p-4 border-b bg-slate-50 flex justify-between items-center">
                            <h3 className="font-bold text-slate-800">Add Alias for {selectedSkill.canonicalName}</h3>
                            <button onClick={closeModal} className="text-slate-400 hover:text-slate-600">
                                <X size={20} />
                            </button>
                        </div>
                        <div className="p-6">
                            <label className="block text-sm font-medium text-slate-700 mb-2">
                                New Alias (comma separated)
                            </label>
                            <input
                                type="text"
                                className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="e.g. handyman, fixer"
                                value={newAlias}
                                onChange={(e) => setNewAlias(e.target.value)}
                                autoFocus
                            />
                            <p className="text-xs text-slate-500 mt-2">
                                These aliases will be indexed for search immediately.
                            </p>
                        </div>
                        <div className="p-4 border-t bg-slate-50 flex justify-end gap-2">
                            <button
                                onClick={closeModal}
                                className="px-4 py-2 text-slate-600 hover:bg-slate-200 rounded-lg transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleAddAlias}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                            >
                                <Save size={16} />
                                Save Alias
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Skills;
