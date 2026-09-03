package com.petlink.modules.admin.vo;

import java.util.Map;

public class AdminStatsOverviewResponse {
    private final Map<String,Long> users;
    private final Map<String,Long> rescueClues;
    private final Map<String,Long> rescueTasks;
    private final Map<String,Long> animals;
    private final Map<String,Long> adoptionApplications;
    private final Map<String,Long> adoptionRecords;
    private final Map<String,Long> followUps;

    public AdminStatsOverviewResponse(Map<String,Long> users,Map<String,Long> rescueClues,Map<String,Long> rescueTasks,
                                      Map<String,Long> animals,Map<String,Long> adoptionApplications,
                                      Map<String,Long> adoptionRecords,Map<String,Long> followUps) {
        this.users=users;this.rescueClues=rescueClues;this.rescueTasks=rescueTasks;this.animals=animals;
        this.adoptionApplications=adoptionApplications;this.adoptionRecords=adoptionRecords;this.followUps=followUps;
    }
    public Map<String,Long> getUsers(){return users;} public Map<String,Long> getRescueClues(){return rescueClues;}
    public Map<String,Long> getRescueTasks(){return rescueTasks;} public Map<String,Long> getAnimals(){return animals;}
    public Map<String,Long> getAdoptionApplications(){return adoptionApplications;}
    public Map<String,Long> getAdoptionRecords(){return adoptionRecords;} public Map<String,Long> getFollowUps(){return followUps;}
}
