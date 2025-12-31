package com.workly.search.service;

import com.workly.search.model.Skill;
import com.workly.search.model.SkillDocument;
import com.workly.search.repository.mongo.SkillRepository;
import com.workly.search.repository.search.SkillSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillSyncService {

    private final SkillRepository skillRepository;
    private final SkillSearchRepository skillSearchRepository;

    public void syncAll() {
        log.info("Starting Full Sync from Mongo to Elasticsearch with Seeding...");

        if (skillRepository.count() == 0) {
            seedInitialSkills();
        }

        List<Skill> skills = skillRepository.findAll();
        List<SkillDocument> documents = skills.stream().map(this::toDocument).collect(Collectors.toList());
        skillSearchRepository.saveAll(documents);

        log.info("Synced {} skills to Elasticsearch", documents.size());
    }

    private void seedInitialSkills() {
        createSkill("SK_ELECTRICIAN", "Electrician", List.of("wiring expert", "lineman", "electrican"), "ELKTRXN");
        createSkill("SK_PLUMBER", "Plumber", List.of("pipe fitter", "plumer"), "PLMBR");
        createSkill("SK_CARPENTER", "Carpenter", List.of("wood worker", "carpenter", "furniture maker"), "KRPNTR");
        createSkill("SK_MECHANIC", "Mechanic", List.of("auto repair", "engine expert"), "MKNK");
    }

    private void createSkill(String id, String name, List<String> aliases, String phonetic) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setCanonicalName(name);
        skill.setAliases(aliases);
        skill.setPhonetic(phonetic);
        skill.setStatus("ACTIVE");
        skillRepository.save(skill);
    }

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    public void addAliases(String canonicalName, List<String> newAliases) {
        Skill skill = skillRepository.findByCanonicalName(canonicalName)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + canonicalName));

        List<String> currentAliases = skill.getAliases();
        if (currentAliases == null) {
            currentAliases = new java.util.ArrayList<>();
        }

        for (String alias : newAliases) {
            if (!currentAliases.contains(alias)) {
                currentAliases.add(alias);
            }
        }

        skill.setAliases(currentAliases);
        skillRepository.save(skill);

        // Sync to Elastic
        skillSearchRepository.save(toDocument(skill));
        log.info("Added aliases {} to skill {}", newAliases, canonicalName);
    }

    private SkillDocument toDocument(Skill skill) {
        SkillDocument doc = new SkillDocument();
        doc.setId(skill.getId());
        doc.setCanonicalName(skill.getCanonicalName());
        doc.setAliases(skill.getAliases());
        doc.setPhonetic(skill.getPhonetic());
        return doc;
    }
}
