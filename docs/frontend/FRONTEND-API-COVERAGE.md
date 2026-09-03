# Frozen API Frontend Coverage

前端 API Client 覆盖以下 Frozen API：

| API ID | Method | Endpoint |
|---|---|---|
| COMMON-API-01 | POST | `/api/files/temporary` |
| COMMON-MEDIA-01 | GET | `/api/media/rescue-clue-images/{imageId}` |
| COMMON-MEDIA-02 | GET | `/api/media/animal-images/{imageId}` |
| COMMON-MEDIA-03 | GET | `/api/media/follow-up-images/{imageId}` |
| M01-API-01 | POST | `/api/auth/register` |
| M01-API-02 | POST | `/api/auth/login` |
| M01-API-03 | GET | `/api/users/me` |
| M01-API-04 | PATCH | `/api/users/me` |
| M02-API-01 | POST | `/api/rescue-clues/idempotency-keys` |
| M02-API-02 | POST | `/api/rescue-clues` |
| M02-API-03 | GET | `/api/rescue-clues/me` |
| M02-API-04 | GET | `/api/rescue-clues/{id}` |
| M02-API-05 | PATCH | `/api/rescue-clues/{id}` |
| M02-API-06 | POST | `/api/rescue-clues/{id}/images` |
| M02-API-07 | DELETE | `/api/rescue-clues/{id}/images/{imageId}` |
| M02-API-08 | POST | `/api/rescue-clues/{id}/withdraw` |
| M02-API-09 | GET | `/api/admin/rescue-clues` |
| M02-API-10 | POST | `/api/admin/rescue-clues/{id}/audit` |
| M03-API-01 | GET | `/api/rescue-clues/waiting-acceptance` |
| M03-API-02 | POST | `/api/rescue-clues/{id}/accept` |
| M03-API-03 | GET | `/api/rescue-tasks/me` |
| M03-API-04 | GET | `/api/rescue-tasks/{id}` |
| M03-API-05 | POST | `/api/rescue-tasks/{id}/start` |
| M03-API-06 | POST | `/api/rescue-tasks/{id}/records` |
| M03-API-07 | GET | `/api/rescue-tasks/{id}/records` |
| M03-API-08 | POST | `/api/rescue-tasks/{id}/result` |
| M03-API-09 | POST | `/api/admin/rescue-tasks/{id}/cancel` |
| M03-API-10 | POST | `/api/admin/rescue-tasks/{id}/failure-resolution` |
| M04-API-01 | GET | `/api/animals` |
| M04-API-02 | GET | `/api/animals/{id}` |
| M04-API-03 | GET | `/api/animals/responsible/me` |
| M04-API-04 | PATCH | `/api/animals/{id}` |
| M04-API-05 | POST | `/api/animals/{id}/images` |
| M04-API-06 | DELETE | `/api/animals/{id}/images/{imageId}` |
| M04-API-07 | POST | `/api/animals/{id}/health-records` |
| M04-API-08 | GET | `/api/animals/{id}/health-records` |
| M04-API-09 | POST | `/api/animals/{id}/status-actions` |
| M05-API-01 | POST | `/api/animals/{id}/adoption-applications` |
| M05-API-02 | GET | `/api/adoption-applications/me` |
| M05-API-03 | GET | `/api/adoption-applications/{id}` |
| M05-API-04 | POST | `/api/adoption-applications/{id}/withdraw` |
| M05-API-05 | GET | `/api/admin/adoption-applications` |
| M05-API-06 | POST | `/api/admin/adoption-applications/{id}/audit` |
| M05-API-07 | GET | `/api/adoption-records/me` |
| M05-API-08 | GET | `/api/adoption-records/{id}` |
| M05-API-09 | GET | `/api/animals/{id}/adoption-overview` |
| M06-API-01 | POST | `/api/adoption-records/{id}/follow-ups` |
| M06-API-02 | GET | `/api/adoption-records/{id}/follow-ups` |
| M06-API-03 | GET | `/api/follow-ups/{id}` |
| M06-API-04 | GET | `/api/admin/follow-ups` |
| M06-API-05 | GET | `/api/rescuer/follow-ups` |
| M07-API-01 | POST | `/api/animals/{id}/favorite` |
| M07-API-02 | DELETE | `/api/animals/{id}/favorite` |
| M07-API-03 | GET | `/api/favorites/me` |
| M07-API-04 | GET | `/api/announcements` |
| M07-API-05 | GET | `/api/announcements/{id}` |
| M07-API-06 | POST | `/api/admin/announcements` |
| M07-API-07 | GET | `/api/admin/announcements` |
| M07-API-08 | GET | `/api/admin/announcements/{id}` |
| M07-API-09 | PATCH | `/api/admin/announcements/{id}` |
| M07-API-10 | POST | `/api/admin/announcements/{id}/publish` |
| M07-API-11 | POST | `/api/admin/announcements/{id}/withdraw` |
| M08-API-01 | GET | `/api/admin/users` |
| M08-API-02 | GET | `/api/admin/users/{id}` |
| M08-API-03 | POST | `/api/admin/users/{id}/enable` |
| M08-API-04 | POST | `/api/admin/users/{id}/disable` |
| M08-API-05 | POST | `/api/admin/users/{id}/promote-rescuer` |
| M08-API-06 | GET | `/api/admin/rescue-tasks` |
| M08-API-07 | GET | `/api/admin/animals` |
| M08-API-08 | GET | `/api/admin/adoption-records` |
| M08-API-09 | GET | `/api/admin/stats/overview` |
| M08-API-10 | GET | `/api/admin/stats/trends` |

合计：**72** 个 API。