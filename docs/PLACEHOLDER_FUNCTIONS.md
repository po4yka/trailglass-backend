# Placeholder and Stubbed Implementations

The following code paths include functions that are currently placeholders or lack full production-ready logic. They should be revisited for proper implementations before relying on them in production.

## Export service archive building
- **File:** `src/main/kotlin/com/trailglass/backend/export/DefaultExportService.kt`
- **Function:** `buildArchive`
- **Gap:** When generating ZIP exports, the method only writes metadata to the archive and includes a TODO for fetching and bundling actual user data (locations, trips, place visits, photos, etc.).

## Stub domain services
- **File:** `src/main/kotlin/com/trailglass/backend/stubs/StubDomainServices.kt`
- **Context:** These classes fulfill service interfaces with hardcoded or empty responses for development/testing and do not implement real domain logic.
- **Functions with stub behavior:**
  - `StubSyncService.getStatus`, `applyDelta`, and `resolveConflict` return default sync statuses and artificial conflict resolutions.
  - `StubLocationService.upsertBatch`, `getLocations`, and `deleteLocations` respond with counts or empty lists without persisting or retrieving location data.
  - `StubTripService.upsertTrip`, `listTrips`, and `deleteTrip` echo or fabricate trip records instead of interacting with storage.
  - `StubPhotoService.requestUpload`, `confirmUpload`, and `fetchMetadata` provide mock presigned URLs and metadata rather than managing uploads.
  - `StubSettingsService.getSettings` and `updateSettings` return in-memory values without persistence.
  - `StubExportService.requestExport` and `getStatus` produce static export job data rather than executing or querying real exports.
