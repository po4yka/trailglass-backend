# Trailglass Admin Panel

Web-based admin panel for managing the Trailglass backend, built with [Refine](https://refine.dev/) and React.

## Features

- User management (view, edit, create)
- Trip management with date range filtering
- Place visit tracking and categorization
- Location data visualization
- JWT-based authentication
- Real-time data from Ktor REST API

## Tech Stack

- **Refine** - React meta-framework for admin panels
- **React 18** - UI library
- **TypeScript** - Type-safe development
- **Ant Design** - UI component library
- **Vite** - Build tool and dev server

## Prerequisites

- Node.js 18+ or Docker Compose
- Trailglass backend running on `http://localhost:8080`

## Getting Started

### Option 1: Using Docker Compose (Recommended)

```bash
# From the project root
docker compose --profile admin up -d
```

The admin panel will be available at `http://localhost:3000`

### Option 2: Local Development

```bash
cd admin
npm install
npm run dev
```

## Configuration

Environment variables are configured in `admin/.env`:

```env
VITE_API_URL=http://localhost:8080/api/v1
```

For production deployment, update this to your production API URL.

## Default Credentials

Use the credentials you set up in your backend database. For development, create a user in the database:

```sql
INSERT INTO users (email, full_name) VALUES ('admin@trailglass.local', 'Admin User') RETURNING id;
INSERT INTO user_credentials (user_id, password_hash)
VALUES ('<user_id>', '<argon2-hash>');
```

Generate Argon2 hash:
```bash
argon2 yourpassword -id -t 3 -m 15 -p 4 -e
```

## Available Pages

### Dashboard
- Overview statistics
- Quick access to resources

### Users
- List all users with filtering
- View user details
- Edit user information
- Create new users

### Trips
- Manage trips with dates and locations
- View trip details and statistics
- Create and edit trips

### Place Visits
- Browse all place visits
- Filter by category and favorites
- View detailed visit information
- Edit visit details

### Locations
- View location tracking data
- Display GPS coordinates and accuracy
- Read-only access to historical data

## Building for Production

```bash
npm run build
```

The built files will be in `admin/dist` and can be served with any static file server:

```bash
npm run preview
```

## Project Structure

```
admin/
├── src/
│   ├── pages/          # Page components (users, trips, etc.)
│   ├── providers/      # Auth and data providers
│   ├── components/     # Reusable components
│   ├── App.tsx         # Main app configuration
│   └── main.tsx        # Entry point
├── public/             # Static assets
├── package.json        # Dependencies
├── vite.config.ts      # Vite configuration
└── tsconfig.json       # TypeScript configuration
```

## API Integration

The admin panel connects to your Ktor backend via the data provider in `src/providers/dataProvider.ts`. It automatically:

- Adds JWT tokens to all requests
- Includes required headers (`X-Device-ID`, `X-App-Version`)
- Handles authentication errors
- Supports pagination and filtering

## Customization

### Adding New Resources

1. Create page components in `src/pages/<resource>/`
2. Add resource configuration in `src/App.tsx`:

```typescript
{
  name: "resource-name",
  list: "/resource-name",
  show: "/resource-name/show/:id",
  edit: "/resource-name/edit/:id",
  create: "/resource-name/create",
  meta: {
    icon: <YourIcon />,
  },
}
```

### Styling

The admin panel uses Ant Design. Customize the theme by modifying the Ant Design configuration in `src/App.tsx`.

## Troubleshooting

**Connection refused to backend**
- Ensure the backend is running on port 8080
- Check CORS configuration in the backend
- Verify `VITE_API_URL` in `.env`

**Authentication errors**
- Check that you're using valid credentials
- Ensure JWT tokens are being stored in localStorage
- Verify the backend `/auth/login` endpoint is working

**Build errors**
- Clear `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Check Node.js version: `node --version` (should be 18+)

## License

Same as the main Trailglass project.
