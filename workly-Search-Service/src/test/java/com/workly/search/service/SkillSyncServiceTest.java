package com.workly.search.service;

import com.workly.search.model.Skill;
import com.workly.search.model.SkillDocument;
import com.workly.search.repository.mongo.SkillRepository;
import com.workly.search.repository.search.SkillSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillSyncServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillSearchRepository skillSearchRepository;

    private SkillSyncService skillSyncService;

    @BeforeEach
    void setUp() {
        skillSyncService = new SkillSyncService(skillRepository, skillSearchRepository);
    }

    @Test
    void syncAll_shouldSeedInitialSkills_whenRepositoryIsEmpty() {
        when(skillRepository.count()).thenReturn(0L);
        when(skillRepository.findAll()).thenReturn(List.of());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        skillSyncService.syncAll();

        // 4 initial skills seeded
        verify(skillRepository, times(4)).save(any(Skill.class));
        verify(skillSearchRepository).saveAll(anyList());
    }

    @Test
    void syncAll_shouldSkipSeeding_whenRepositoryAlreadyHasData() {
        when(skillRepository.count()).thenReturn(4L);
        List<Skill> existing = List.of(skill("SK1", "Electrician", List.of(), "ELKTRXN"));
        when(skillRepository.findAll()).thenReturn(existing);

        skillSyncService.syncAll();

        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void syncAll_shouldSyncAllSkillsToElasticsearch() {
        when(skillRepository.count()).thenReturn(2L);
        List<Skill> skills = List.of(
                skill("SK1", "Electrician", List.of("lineman"), "ELKTRXN"),
                skill("SK2", "Plumber", List.of("pipe fitter"), "PLMBR"));
        when(skillRepository.findAll()).thenReturn(skills);

        skillSyncService.syncAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SkillDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillSearchRepository).saveAll(captor.capture());
        List<SkillDocument> docs = captor.getValue();
        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getCanonicalName()).isEqualTo("Electrician");
        assertThat(docs.get(1).getCanonicalName()).isEqualTo("Plumber");
    }

    @Test
    void getAllSkills_shouldReturnAllFromRepository() {
        List<Skill> skills = List.of(
                skill("SK1", "Electrician", List.of(), "E"),
                skill("SK2", "Plumber", List.of(), "P"));
        when(skillRepository.findAll()).thenReturn(skills);

        List<Skill> result = skillSyncService.getAllSkills();

        assertThat(result).hasSize(2);
    }

    @Test
    void addAliases_shouldAddNewAliasesAndSync() {
        Skill existing = skill("SK1", "Electrician", new ArrayList<>(List.of("wiring expert")), "E");
        when(skillRepository.findByCanonicalName("Electrician")).thenReturn(Optional.of(existing));
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        skillSyncService.addAliases("Electrician", List.of("lineman", "electrican"));

        assertThat(existing.getAliases()).containsExactlyInAnyOrder("wiring expert", "lineman", "electrican");
        verify(skillRepository).save(existing);
        verify(skillSearchRepository).save(any(SkillDocument.class));
    }

    @Test
    void addAliases_shouldNotDuplicateExistingAlias() {
        Skill existing = skill("SK1", "Electrician", new ArrayList<>(List.of("wiring expert")), "E");
        when(skillRepository.findByCanonicalName("Electrician")).thenReturn(Optional.of(existing));
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        skillSyncService.addAliases("Electrician", List.of("wiring expert", "lineman"));

        assertThat(existing.getAliases()).containsExactlyInAnyOrder("wiring expert", "lineman");
    }

    @Test
    void addAliases_shouldInitializeAliasesList_whenSkillHasNullAliases() {
        Skill existing = skill("SK1", "Electrician", null, "E");
        when(skillRepository.findByCanonicalName("Electrician")).thenReturn(Optional.of(existing));
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        skillSyncService.addAliases("Electrician", List.of("lineman"));

        assertThat(existing.getAliases()).containsExactly("lineman");
    }

    @Test
    void addAliases_shouldThrow_whenSkillNotFound() {
        when(skillRepository.findByCanonicalName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillSyncService.addAliases("Unknown", List.of("alias")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Skill not found: Unknown");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Skill skill(String id, String name, List<String> aliases, String phonetic) {
        Skill s = new Skill();
        s.setId(id);
        s.setCanonicalName(name);
        s.setAliases(aliases);
        s.setPhonetic(phonetic);
        s.setStatus("ACTIVE");
        return s;
    }
}
