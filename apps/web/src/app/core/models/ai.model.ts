import { EventFormat, EventSummary } from './event.model';

export interface AiDraftEventRequest {
  prompt: string;
  groupContext?: string;
  topicPreference?: string;
}

export interface AiDraftEventResponse {
  title: string;
  description: string;
  format: EventFormat;
  suggestedCategorySlug: string;
  suggestedTopicSlugs: string[];
  estimatedCapacity?: number;
  durationMinutes?: number;
}

export interface AiFeedbackSummaryResponse {
  overallSentiment: string;
  averageRating: number;
  topThemes: string[];
  positiveHighlights: string[];
  improvementSuggestions: string[];
  narrativeSummary: string;
}

export interface AiRecommendationsResponse {
  recommendedEvents: EventSummary[];
  rationale: string;
}
