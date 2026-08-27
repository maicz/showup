import { MemberSummary } from './member.model';

export interface PhotoSummary {
  id: string;
  uploadedBy: MemberSummary;
  url: string;
  caption?: string;
  width: number;
  height: number;
  createdAt: string;
}

export interface UploadPhotoRequest {
  url: string;
  caption?: string;
  width: number;
  height: number;
}
