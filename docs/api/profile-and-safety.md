# Profile And Safety API

Base path: `/api/v1/profile`

All endpoints require a valid access token. Cookie-authenticated writes also require CSRF proof. The server derives ownership from the JWT subject; request bodies never contain or override `userId`.

## Endpoints

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/v1/profile` | returns the authenticated user's profile |
| `PUT` | `/api/v1/profile` | creates or replaces the authenticated user's calculation profile |
| `POST` | `/api/v1/profile/screenings` | appends an immutable screening version and returns its planning gate |
| `GET` | `/api/v1/profile/screenings/current` | returns the authenticated user's latest screening version |

Profile input is deliberately minimized:

```json
{
  "dateOfBirth": "1992-04-20",
  "calculationSex": "MALE",
  "heightCm": 178,
  "currentWeightKg": 82,
  "targetWeightKg": 74,
  "activityLevel": "LIGHT",
  "timeZone": "Asia/Hong_Kong"
}
```

Supported activity values are `SEDENTARY`, `LIGHT`, `MODERATE`, and `VERY_ACTIVE`. Measurements use centimetres and kilograms. The service accepts heights from 100 to 250 cm and weights from 30 to 350 kg, rejects non-finite values, and requires a valid IANA time zone.

Screening input contains only self-reported routing flags:

```json
{
  "pregnantOrBreastfeeding": false,
  "eatingDisorderHistory": false,
  "medicalGuidanceRequired": false,
  "weightAffectingMedication": true,
  "concerningSymptoms": false
}
```

The response returns `ELIGIBLE`, `PROFESSIONAL_REVIEW`, or `INELIGIBLE`, an `automaticPlanningAllowed` gate, stable reason codes, and non-diagnostic guidance. Users under 18 are outside this adult product. Any reported risk pauses automatic planning; later planning services must enforce this persisted gate rather than relying on UI text or a prompt.

## Stable Errors

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `INVALID_PROFILE` | profile values are outside supported bounds |
| 401 | `UNAUTHENTICATED` | no valid access token |
| 403 | `FORBIDDEN` | CSRF proof or permission is missing |
| 404 | `PROFILE_NOT_FOUND` | the authenticated user's requested record does not exist |
| 409 | `PROFILE_REQUIRED` | screening was requested before a profile exists |
