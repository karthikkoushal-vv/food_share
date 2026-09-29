# FoodShare API

Spring Boot REST API for sharing surplus food between donors and NGOs. It uses Java 17, Spring Data JPA, and MySQL schema `food_share`.

## Run

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Start the MySQL Server service and make sure it accepts connections on `localhost:3306`. MySQL Workbench is the client; the application connects directly to the MySQL Server. The JDBC URL creates the `food_share` schema if the configured MySQL user has permission. Hibernate creates or updates the application tables on startup.

The default local username/password are `root`/`root`. If your MySQL credentials differ, set them in PowerShell before starting the app:

```powershell
$env:FOOD_SHARE_DB_USERNAME = "root"
$env:FOOD_SHARE_DB_PASSWORD = "your-MySQL-password"
.\mvnw.cmd spring-boot:run
```

In Workbench, connect to the same MySQL Server at `localhost:3306` and refresh the Schemas pane to see `food_share` and its tables. Integration tests use a separate in-memory database and do not modify the MySQL schema.

Open Swagger UI at `http://localhost:8080/swagger-ui/index.html`. The OpenAPI JSON is at `http://localhost:8080/v3/api-docs`.

The dashboard reloads profiles, listings, claims, and impact every 20 seconds while the page is visible, and refreshes when you return to the tab. Changes submitted through the website are saved immediately; changes made directly in MySQL appear on the next refresh.

## API workflow

1. Register donor and NGO records with `POST /api/donors` and `POST /api/ngos`, each using `{"name":"Campus Kitchen"}`.
2. Create a food listing with `POST /api/listings`. Include `donorId`, `name`, `foodType`, positive `quantity`, `unit`, `pickupArea`, and a future `safeUntil` timestamp.
3. Browse unexpired listings with `GET /api/listings`; add the `area` query parameter to match pickup areas, for example `GET /api/listings?area=North%20Campus`.
4. Claim a listing with `POST /api/listings/{listingId}/claims` and body `{"ngoId":1}`.
5. Confirm pickup with `PATCH /api/claims/{claimId}/collected`.
6. See collected food totals grouped by month and unit with `GET /api/statistics/diverted/monthly`.

The Swagger page includes endpoint descriptions, request examples, and input field details. Invalid requests return HTTP 400, missing records return 404, and listings that are claimed, collected, or expired return HTTP 409.


