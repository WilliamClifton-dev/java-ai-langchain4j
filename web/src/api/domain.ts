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
