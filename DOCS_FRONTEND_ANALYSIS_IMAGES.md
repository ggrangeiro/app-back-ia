# Documentation - Analysis Image Storage Integration

**Version:** 1.0
**Status:** Ready for Integration

This document outlines the API changes and integration workflow for attaching images to user analyses (e.g., Posture Analysis).

## Overview
The workflow requires two steps:
1.  **Upload the Image**: Upload the file to the new `analysis_evidence` endpoint to get a public URL.
2.  **Save Analysis**: Include the returned URL in the `result` object when saving the analysis history.

---

## 1. Upload API
**Endpoint:** `POST /api/usuarios/{userId}/upload-asset`

### Changes
- New allowed value for `type`: `"analysis_evidence"`.

### Request
**Headers:**
- `Content-Type`: `multipart/form-data`

**Query Parameters:**
- `requesterId`: ID of the user performing the action.
- `requesterRole`: Role (`USER`, `PERSONAL`, `ADMIN`).

**Body (FormData):**
- `file`: The image file (Binary, max 2MB, JPG/PNG).
- `type`: `"analysis_evidence"` (Required).

### Response
```json
{
  "success": true,
  "imageUrl": "https://storage.googleapis.com/imagem-ai/uploads/users/123/analysis/1705512345678_uuid.jpg"
}
```

### Frontend Example (bussiness logic)
```typescript
async function uploadAnalysisImage(userId: string, file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', 'analysis_evidence');

  const response = await api.post(`/usuarios/${userId}/upload-asset`, formData, {
    params: {
      requesterId: currentUser.id, // or similar
      requesterRole: currentUser.role
    }
  });

  return response.data.imageUrl;
}
```

---

## 2. History API (Save Analysis)
**Endpoint:** `POST /api/historico`

### Changes
- The `result` JSON object now supports an `imageUrl` field.

### Request Body
```json
{
  "userId": "123",
  "exercise": "POSTURE_ANALYSIS",
  "result": {
    "score": 85,
    "feedback": [{ "message": "Good posture", "score": 90 }],
    "imageUrl": "https://storage.googleapis.com/.../analysis_evidence.jpg" // <--- NEW FIELD
  }
}
```

### TypeScript Interface Update
Update your `AnalysisResult` type definition to include the new field.

```typescript
export interface AnalysisResult {
  isValidContent: boolean;
  score: number;
  repetitions?: number;
  gender?: string; // e.g., "Male", "Female"
  formCorrection?: string;
  feedback: { message: string; score: number }[];
  strengths: string[];
  improvements: { instruction: string; detail: string }[];
  muscleGroups: string[];
  voiceFeedback?: string;
  
  // New Fields
  imageUrl?: string;      // Primary analysis image
  imageUrls?: string[];   // Optional: For multi-image analyses
}
```

---

## 3. Integration Workflow

1.  **User Captures Image**: User takes a photo or uploads one for analysis.
2.  **Upload First**: Frontend calls `uploadAnalysisImage(userId, file)`.
3.  **Get URL**: Receive `imageUrl` from the response.
4.  **Perform Analysis**: (Optional) If you send the image to an AI service for analysis, do that here.
5.  **Save Result**: calls `saveHistory(analysisData)` including the `imageUrl` in the `result` object.
6.  **Display**: When fetching history (`GET /api/historico/{userId}`), the `result` object will contain the `imageUrl` for display.
