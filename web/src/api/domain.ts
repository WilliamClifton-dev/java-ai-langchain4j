export type CalculationSex = 'FEMALE' | 'MALE';
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'VERY_ACTIVE';

export interface ProfileInput {
  dateOfBirth: string;
  calculationSex: CalculationSex;
  heightCm: number;
  currentWeightKg: number;
  targetWeightKg: number;
  activityLevel: ActivityLevel;
  timeZone: string;
}

export interface Profile extends ProfileInput {
  userId: string;
}

export interface ScreeningInput {
  pregnantOrBreastfeeding: boolean;
  eatingDisorderHistory: boolean;
  medicalGuidanceRequired: boolean;
  weightAffectingMedication: boolean;
  concerningSymptoms: boolean;
}

export interface SafetyScreening {
  id: string;
  version: number;
  status: 'ELIGIBLE' | 'PROFESSIONAL_REVIEW' | 'INELIGIBLE';
  automaticPlanningAllowed: boolean;
  reasonCodes: string[];
  guidance: string;
  createdAt: string;
}

export interface HbtiDefinition {
  version: string;
  displayName: string;
  answerMin: number;
  answerMax: number;
  dimensions: Array<{
    code: string;
    ordinal: number;
    leftPole: string;
    rightPole: string;
    leftLabel: string;
    rightLabel: string;
    descriptionZh: string;
    descriptionEn: string;
  }>;
  items: Array<{
    itemKey: string;
    ordinal: number;
    titleZh: string;
    hintZh: string;
    titleEn: string;
    hintEn: string;
  }>;
  limitation: string;
}

export interface HbtiResult {
  id: string;
  definitionVersion: string;
  scoringRuleVersion: string;
  dimensions: Array<{
    dimensionCode: string;
    chosenPole: string;
    leftScore: number;
    rightScore: number;
  }>;
  typeCode: string;
  limitation: string;
  completedAt: string;
}

export interface WeightPlan {
  id: string;
  planId: string;
  versionNo: number;
  status: 'DRAFT' | 'VALIDATED' | 'CONFIRMED' | 'ACTIVE' | 'REPLACED';
  goal: 'LOSS' | 'MAINTENANCE' | 'GAIN';
  formulaVersion: string;
  targetPolicyVersion: string;
  bmi: number;
  bmrKcalPerDay: number;
  tdeeKcalPerDay: number;
  energyMinKcalPerDay: number;
  energyMaxKcalPerDay: number;
  weeklyWeightChangeMinPercent: number;
  weeklyWeightChangeMaxPercent: number;
  createdAt: string;
  validatedAt: string | null;
  confirmedAt: string | null;
  activatedAt: string | null;
  replacedAt: string | null;
  guidance: string;
}

export interface DailyMetricInput {
  localDate: string;
  weightKg?: number;
  steps?: number;
  activityMinutes?: number;
  sleepMinutes?: number;
  sleepQuality?: number;
}

export interface DailyMetric extends DailyMetricInput {
  id: string;
  createdAt: string;
}

export interface NutritionInput {
  localDate: string;
  energyKcal: number;
  proteinG: number;
  carbohydrateG: number;
  fatG: number;
}

export interface NutritionLog extends NutritionInput {
  id: string;
  createdAt: string;
}

export type TrainingType = 'STRENGTH' | 'CARDIO' | 'MOBILITY' | 'SPORT' | 'OTHER';
export type TrainingIntensity = 'LOW' | 'MODERATE' | 'HIGH';

export interface TrainingInput {
  localDate: string;
  trainingType: TrainingType;
  durationMinutes: number;
  intensity: TrainingIntensity;
}

export interface TrainingLog extends TrainingInput {
  id: string;
  createdAt: string;
}

export interface TrackingWrite<T> {
  record: T;
  replayed: boolean;
}

export interface DailySummary {
  localDate: string;
  metric: DailyMetric | null;
  nutrition: NutritionLog | null;
  trainingSessions: TrainingLog[];
  trainingMinutes: number;
}

export type WeeklyReviewRecommendation =
  | 'INSUFFICIENT_DATA'
  | 'HOLD'
  | 'INCREASE_ENERGY'
  | 'DECREASE_ENERGY';

export interface WeeklyReview {
  id: string;
  planVersionId: string;
  windowStart: string;
  windowEnd: string;
  versionNo: number;
  policyVersion: string;
  weightObservationDays: number;
  nutritionLoggedDays: number;
  stepsObservedDays: number;
  sleepObservedDays: number;
  trainingDays: number;
  averageWeightKg: number | null;
  weightTrendPercent: number | null;
  nutritionAdherencePercent: number | null;
  averageSteps: number | null;
  averageSleepMinutes: number | null;
  totalTrainingMinutes: number;
  recommendation: WeeklyReviewRecommendation;
  proposedEnergyDeltaKcalPerDay: -100 | 0 | 100;
  reason: string;
  createdAt: string;
  limitation: string;
}

export interface WeeklyReviewWrite {
  review: WeeklyReview;
  replayed: boolean;
}

export type CoachScene =
  | 'GENERAL_CHAT'
  | 'PLAN_GENERATION'
  | 'DAILY_CHECKIN'
  | 'WEEKLY_REVIEW'
  | 'HBTI_INTERPRETATION'
  | 'SAFETY_SCREENING';

export interface CoachStreamInput {
  conversationId: string;
  scene: CoachScene;
  message: string;
}

export type CoachStreamEvent =
  | { type: 'metadata'; conversationId: string; scene: CoachScene }
  | { type: 'token'; sequence: number; text: string }
  | { type: 'completion'; conversationId: string }
  | { type: 'error'; code: string; message: string; retryable: boolean };
