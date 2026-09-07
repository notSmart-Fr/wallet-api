# wallet-app

A wallet monorepo with a Spring Boot backend and a Next.js frontend.

## Repository layout

- `backend/` - Java 25 Spring Boot API, Maven wrapper, tests, and backend Dockerfile.
- `frontend/` - Next.js 16 application and frontend tests.
- `docker-compose.yml` - Local PostgreSQL and PgAdmin services.
- `specs/` - Feature specifications, contracts, and implementation tasks.

## Local development

Start the shared database services:

```powershell
docker compose up -d
```

Run the backend from the repository root:

```powershell
$env:SUPABASE_PROJECT_REF = "<your-supabase-project-ref>"
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Run the frontend in a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

The API is available at `http://localhost:8080`, its Scalar documentation at
`http://localhost:8080/docs`, and the frontend at `http://localhost:3000`.

## Backend container

Build and run the backend image from the repository root:

```powershell
docker build -f backend/Dockerfile -t wallet-api-backend backend
docker run --rm -p 8080:8080 wallet-api-backend
```
