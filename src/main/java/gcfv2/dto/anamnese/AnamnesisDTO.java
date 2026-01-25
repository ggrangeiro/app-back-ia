package gcfv2.dto.anamnese;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public class AnamnesisDTO {

    private String updatedAt;

    // Texto bruto extraído de documento via IA (opcional)
    private String extractedDocumentText;

    private PersonalInfo personal;
    private MarketingInfo marketing;
    private PhysicalInfo physical;
    private GoalsInfo goals;
    private HealthInfo health;
    private NutritionInfo nutrition;
    private FitnessInfo fitness;
    private PreferencesInfo preferences;
    private ClosingInfo closing;

    // Getters and Setters
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getExtractedDocumentText() {
        return extractedDocumentText;
    }

    public void setExtractedDocumentText(String extractedDocumentText) {
        this.extractedDocumentText = extractedDocumentText;
    }

    public PersonalInfo getPersonal() {
        return personal;
    }

    public void setPersonal(PersonalInfo personal) {
        this.personal = personal;
    }

    public MarketingInfo getMarketing() {
        return marketing;
    }

    public void setMarketing(MarketingInfo marketing) {
        this.marketing = marketing;
    }

    public PhysicalInfo getPhysical() {
        return physical;
    }

    public void setPhysical(PhysicalInfo physical) {
        this.physical = physical;
    }

    public GoalsInfo getGoals() {
        return goals;
    }

    public void setGoals(GoalsInfo goals) {
        this.goals = goals;
    }

    public HealthInfo getHealth() {
        return health;
    }

    public void setHealth(HealthInfo health) {
        this.health = health;
    }

    public NutritionInfo getNutrition() {
        return nutrition;
    }

    public void setNutrition(NutritionInfo nutrition) {
        this.nutrition = nutrition;
    }

    public FitnessInfo getFitness() {
        return fitness;
    }

    public void setFitness(FitnessInfo fitness) {
        this.fitness = fitness;
    }

    public PreferencesInfo getPreferences() {
        return preferences;
    }

    public void setPreferences(PreferencesInfo preferences) {
        this.preferences = preferences;
    }

    public ClosingInfo getClosing() {
        return closing;
    }

    public void setClosing(ClosingInfo closing) {
        this.closing = closing;
    }

    @Serdeable
    public static class PersonalInfo {
        private String fullName;
        private String whatsapp;
        private String birthDate; // YYYY-MM-DD
        private Integer age;
        private LocationInfo location;
        private String maritalStatus;
        private String profession;
        private String gender; // Masculino ou Feminino

        // Getters/Setters
        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getWhatsapp() {
            return whatsapp;
        }

        public void setWhatsapp(String whatsapp) {
            this.whatsapp = whatsapp;
        }

        public String getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(String birthDate) {
            this.birthDate = birthDate;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public LocationInfo getLocation() {
            return location;
        }

        public void setLocation(LocationInfo location) {
            this.location = location;
        }

        public String getMaritalStatus() {
            return maritalStatus;
        }

        public void setMaritalStatus(String maritalStatus) {
            this.maritalStatus = maritalStatus;
        }

        public String getProfession() {
            return profession;
        }

        public void setProfession(String profession) {
            this.profession = profession;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }
    }

    @Serdeable
    public static class LocationInfo {
        private String city;
        private String state;
        private String country;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    @Serdeable
    public static class MarketingInfo {
        private String referralSource;
        private String instagramFollowTime;

        public String getReferralSource() {
            return referralSource;
        }

        public void setReferralSource(String referralSource) {
            this.referralSource = referralSource;
        }

        public String getInstagramFollowTime() {
            return instagramFollowTime;
        }

        public void setInstagramFollowTime(String instagramFollowTime) {
            this.instagramFollowTime = instagramFollowTime;
        }
    }

    @Serdeable
    public static class PhysicalInfo {
        private Float weight;
        private Float height;
        private Float targetWeight;
        private Integer currentBodyShape;
        private Integer desiredBodyShape;
        private String bodyDissatisfaction;

        public Float getWeight() {
            return weight;
        }

        public void setWeight(Float weight) {
            this.weight = weight;
        }

        public Float getHeight() {
            return height;
        }

        public void setHeight(Float height) {
            this.height = height;
        }

        public Float getTargetWeight() {
            return targetWeight;
        }

        public void setTargetWeight(Float targetWeight) {
            this.targetWeight = targetWeight;
        }

        public Integer getCurrentBodyShape() {
            return currentBodyShape;
        }

        public void setCurrentBodyShape(Integer currentBodyShape) {
            this.currentBodyShape = currentBodyShape;
        }

        public Integer getDesiredBodyShape() {
            return desiredBodyShape;
        }

        public void setDesiredBodyShape(Integer desiredBodyShape) {
            this.desiredBodyShape = desiredBodyShape;
        }

        public String getBodyDissatisfaction() {
            return bodyDissatisfaction;
        }

        public void setBodyDissatisfaction(String bodyDissatisfaction) {
            this.bodyDissatisfaction = bodyDissatisfaction;
        }
    }

    @Serdeable
    public static class GoalsInfo {
        private String threeMonthGoal;
        private String mainObstacle;

        public String getThreeMonthGoal() {
            return threeMonthGoal;
        }

        public void setThreeMonthGoal(String threeMonthGoal) {
            this.threeMonthGoal = threeMonthGoal;
        }

        public String getMainObstacle() {
            return mainObstacle;
        }

        public void setMainObstacle(String mainObstacle) {
            this.mainObstacle = mainObstacle;
        }
    }

    @Serdeable
    public static class HealthInfo {
        private List<String> conditions;
        private String injuries;
        private String lastCheckup;
        private Boolean chestPain;
        private String dailyActivity;
        private String sleepQuality;

        public List<String> getConditions() {
            return conditions;
        }

        public void setConditions(List<String> conditions) {
            this.conditions = conditions;
        }

        public String getInjuries() {
            return injuries;
        }

        public void setInjuries(String injuries) {
            this.injuries = injuries;
        }

        public String getLastCheckup() {
            return lastCheckup;
        }

        public void setLastCheckup(String lastCheckup) {
            this.lastCheckup = lastCheckup;
        }

        public Boolean getChestPain() {
            return chestPain;
        }

        public void setChestPain(Boolean chestPain) {
            this.chestPain = chestPain;
        }

        public String getDailyActivity() {
            return dailyActivity;
        }

        public void setDailyActivity(String dailyActivity) {
            this.dailyActivity = dailyActivity;
        }

        public String getSleepQuality() {
            return sleepQuality;
        }

        public void setSleepQuality(String sleepQuality) {
            this.sleepQuality = sleepQuality;
        }
    }

    @Serdeable
    public static class NutritionInfo {
        private Boolean nutritionalMonitoring;
        private String eatingHabits;

        public Boolean getNutritionalMonitoring() {
            return nutritionalMonitoring;
        }

        public void setNutritionalMonitoring(Boolean nutritionalMonitoring) {
            this.nutritionalMonitoring = nutritionalMonitoring;
        }

        public String getEatingHabits() {
            return eatingHabits;
        }

        public void setEatingHabits(String eatingHabits) {
            this.eatingHabits = eatingHabits;
        }
    }

    @Serdeable
    public static class FitnessInfo {
        private Boolean currentlyExercising;
        private String currentActivity;
        private String timePracticing;
        private String timeStopped;
        private String trainingLocation;
        private GymDetailsInfo gymDetails;
        private List<String> homeEquipment;
        private Integer weeklyFrequency;
        private String trainingTimeAvailable;

        public Boolean getCurrentlyExercising() {
            return currentlyExercising;
        }

        public void setCurrentlyExercising(Boolean currentlyExercising) {
            this.currentlyExercising = currentlyExercising;
        }

        public String getCurrentActivity() {
            return currentActivity;
        }

        public void setCurrentActivity(String currentActivity) {
            this.currentActivity = currentActivity;
        }

        public String getTimePracticing() {
            return timePracticing;
        }

        public void setTimePracticing(String timePracticing) {
            this.timePracticing = timePracticing;
        }

        public String getTimeStopped() {
            return timeStopped;
        }

        public void setTimeStopped(String timeStopped) {
            this.timeStopped = timeStopped;
        }

        public String getTrainingLocation() {
            return trainingLocation;
        }

        public void setTrainingLocation(String trainingLocation) {
            this.trainingLocation = trainingLocation;
        }

        public GymDetailsInfo getGymDetails() {
            return gymDetails;
        }

        public void setGymDetails(GymDetailsInfo gymDetails) {
            this.gymDetails = gymDetails;
        }

        public List<String> getHomeEquipment() {
            return homeEquipment;
        }

        public void setHomeEquipment(List<String> homeEquipment) {
            this.homeEquipment = homeEquipment;
        }

        public Integer getWeeklyFrequency() {
            return weeklyFrequency;
        }

        public void setWeeklyFrequency(Integer weeklyFrequency) {
            this.weeklyFrequency = weeklyFrequency;
        }

        public String getTrainingTimeAvailable() {
            return trainingTimeAvailable;
        }

        public void setTrainingTimeAvailable(String trainingTimeAvailable) {
            this.trainingTimeAvailable = trainingTimeAvailable;
        }
    }

    @Serdeable
    public static class GymDetailsInfo {
        private String type;
        private String name;
        private Boolean condoPhotosSent;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getCondoPhotosSent() {
            return condoPhotosSent;
        }

        public void setCondoPhotosSent(Boolean condoPhotosSent) {
            this.condoPhotosSent = condoPhotosSent;
        }
    }

    @Serdeable
    public static class PreferencesInfo {
        private String dislikedExercises;
        private String likedExercises;
        private String cardioPreference;
        private String bodyPartFocus;

        public String getDislikedExercises() {
            return dislikedExercises;
        }

        public void setDislikedExercises(String dislikedExercises) {
            this.dislikedExercises = dislikedExercises;
        }

        public String getLikedExercises() {
            return likedExercises;
        }

        public void setLikedExercises(String likedExercises) {
            this.likedExercises = likedExercises;
        }

        public String getCardioPreference() {
            return cardioPreference;
        }

        public void setCardioPreference(String cardioPreference) {
            this.cardioPreference = cardioPreference;
        }

        public String getBodyPartFocus() {
            return bodyPartFocus;
        }

        public void setBodyPartFocus(String bodyPartFocus) {
            this.bodyPartFocus = bodyPartFocus;
        }
    }

    @Serdeable
    public static class ClosingInfo {
        private String programConfidence;
        private String programAttraction;
        private String salesVideoPoint;

        public String getProgramConfidence() {
            return programConfidence;
        }

        public void setProgramConfidence(String programConfidence) {
            this.programConfidence = programConfidence;
        }

        public String getProgramAttraction() {
            return programAttraction;
        }

        public void setProgramAttraction(String programAttraction) {
            this.programAttraction = programAttraction;
        }

        public String getSalesVideoPoint() {
            return salesVideoPoint;
        }

        public void setSalesVideoPoint(String salesVideoPoint) {
            this.salesVideoPoint = salesVideoPoint;
        }
    }
}
