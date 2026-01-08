# YearReviewTFT 2025

A **Spotify Wrapped–style annual recap** for *Teamfight Tactics* players.  
YearReviewTFT analyzes a player’s full season and presents it as a **mobile-first, swipeable story experience**.

Built with performance, design, and shareability in mind.

---

## Features

- **Full-year TFT statistics**
  - Games played, average placement, Top 4 rate
  - Placements distribution (1st–8th)
  - Most played traits, augments, units, and comps
- **Mobile-first story UI**
  - Vertical swipe navigation (Spotify Wrapped style)
  - Smooth animations and transitions
- **Fast & scalable**
  - Backend caching to minimize Riot API calls
  - Optimized for first-load performance
- **Shareable summary**
  - Dedicated summary page for sharing results
  - Native mobile share support
- **Statically exported frontend**
  - Works on static hosting (no server required for UI)

---

## Architecture Overview

### Backend
- **Java + Spring Boot**
- Riot Games API integration
- PostgreSQL for persistence
- Smart caching layer for match data
- REST API serving aggregated yearly stats

### Frontend
- **Next.js (App Router)**
- Static export (`output: "export"`)
- Tailwind CSS
- Framer Motion animations
- Embla Carousel (vertical story navigation)

---

##  Project Structure

```
YearReviewTFT-2025/
├── backend/
│   ├── src/main/java/...
│   ├── persistence/
│   ├── service/
│   └── api/
│
├── frontend/
│   ├── app/
│   │   ├── wrapped/
│   │   ├── summary/
│   │   └── page.tsx
│   ├── components/
│   ├── lib/
│   └── next.config.js
│
└── README.md
```

---

## ⚙️ Setup & Development

### Prerequisites
- Node.js 18+
- Java 17+
- PostgreSQL
- Riot Games API Key

---

### Backend Setup

```bash
cd backend
./mvnw spring-boot:run
```

Configure environment variables:
```env
RIOT_API_KEY=your_key_here
DATABASE_URL=jdbc:postgresql://localhost:5432/yearreviewtft
```

---

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

For static export:
```bash
npm run build
```

The output will be generated in:
```
frontend/out/
```

---

## User Flow

1. User enters **PUUID + year**
2. Backend fetches and aggregates match data
3. Frontend renders a **vertical story**
4. Final CTA slide:
   - Share summary
   - Search another player
5. Summary page optimized for social sharing

---

##  Key Design Decisions

- **Static export** for maximum hosting flexibility
- **CTA as a story card**, not a floating button
- **No dynamic routes** for user-generated pages (static-safe)
- Clear separation between:
  - Data fetching
  - Story orchestration
  - Presentation components

---

##  API & Rate Limits

- Riot API rate limits are respected via:
  - Match caching
  - Player-year deduplication
- First-time requests may take longer due to initial data fetch

---

##  Tech Stack

- **Frontend:** Next.js, React, Tailwind CSS, Framer Motion, Embla
- **Backend:** Java, Spring Boot, Hibernate
- **Database:** PostgreSQL
- **Deployment:** Static frontend + backend API

---

##  Roadmap

- OpenGraph share images
- Auto-generated recap images
- Region auto-detection
- Multi-year comparisons
- Authenticated user profiles

---

##  License

This project is licensed under the MIT License.

---

## Author

**Tuan Phong Phan**  
TU Darmstadt
