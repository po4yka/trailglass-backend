import { Refine } from "@refinedev/core";
import { RefineKbar, RefineKbarProvider } from "@refinedev/kbar";
import {
  AuthPage,
  ErrorComponent,
  ThemedLayoutV2,
  ThemedSiderV2,
  useNotificationProvider,
} from "@refinedev/antd";
import routerBindings, {
  CatchAllNavigate,
  DocumentTitleHandler,
  NavigateToResource,
  UnsavedChangesNotifier,
} from "@refinedev/react-router-v6";
import { App as AntdApp, ConfigProvider, theme } from "antd";
import { BrowserRouter, Outlet, Route, Routes } from "react-router-dom";
import "@refinedev/antd/dist/reset.css";
import "./App.css";

import { authProvider } from "./providers/authProvider";
import { dataProvider } from "./providers/dataProvider";
import { UserList, UserShow, UserEdit } from "./pages/users";
import { TripList, TripShow, TripEdit, TripCreate } from "./pages/trips";
import { PlaceVisitList, PlaceVisitShow, PlaceVisitEdit } from "./pages/place-visits";
import { LocationList, LocationShow } from "./pages/locations";
import { PhotoList, PhotoShow } from "./pages/photos";
import { ExportList, ExportShow, ExportCreate } from "./pages/exports";
import { DashboardPage } from "./pages/dashboard";
import { Header } from "./components/Header";
import { Footer } from "./components/Footer";
import { Title } from "./components/Title";
import {
  UserOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  PictureOutlined,
  CarOutlined,
  DashboardOutlined,
  FileZipOutlined,
} from "@ant-design/icons";

function App() {
  return (
    <BrowserRouter>
      <RefineKbarProvider>
        <ConfigProvider
          theme={{
            algorithm: theme.defaultAlgorithm,
            token: {
              colorPrimary: "#0ea5e9",
              colorSuccess: "#10b981",
              colorWarning: "#f59e0b",
              colorError: "#ef4444",
              colorInfo: "#06b6d4",
              colorLink: "#0891b2",
              borderRadius: 8,
              fontSize: 14,
              colorBgLayout: "#f0f2f5",
              colorBgContainer: "#ffffff",
              colorBorder: "#e5e7eb",
            },
            components: {
              Layout: {
                headerBg: "#0ea5e9",
                headerHeight: 64,
                headerPadding: "0 24px",
                siderBg: "#ffffff",
                bodyBg: "#f0f2f5",
              },
              Menu: {
                itemSelectedBg: "#e0f2fe",
                itemSelectedColor: "#0c4a6e",
                itemHoverBg: "#f0f9ff",
                itemHoverColor: "#075985",
                iconSize: 18,
              },
              Card: {
                borderRadiusLG: 12,
                paddingLG: 24,
              },
              Button: {
                borderRadius: 8,
                controlHeight: 40,
              },
              Input: {
                borderRadius: 8,
                controlHeight: 40,
              },
            },
          }}
        >
          <AntdApp>
            <Refine
              dataProvider={dataProvider}
              authProvider={authProvider}
              routerProvider={routerBindings}
              notificationProvider={useNotificationProvider}
              resources={[
                {
                  name: "dashboard",
                  list: "/",
                  meta: {
                    label: "Dashboard",
                    icon: <DashboardOutlined />,
                  },
                },
                {
                  name: "users",
                  list: "/users",
                  show: "/users/show/:id",
                  edit: "/users/edit/:id",
                  meta: {
                    icon: <UserOutlined />,
                  },
                },
                {
                  name: "trips",
                  list: "/trips",
                  show: "/trips/show/:id",
                  edit: "/trips/edit/:id",
                  create: "/trips/create",
                  meta: {
                    icon: <CarOutlined />,
                  },
                },
                {
                  name: "place-visits",
                  list: "/place-visits",
                  show: "/place-visits/show/:id",
                  edit: "/place-visits/edit/:id",
                  meta: {
                    label: "Place Visits",
                    icon: <HomeOutlined />,
                  },
                },
                {
                  name: "locations",
                  list: "/locations",
                  show: "/locations/show/:id",
                  meta: {
                    icon: <EnvironmentOutlined />,
                  },
                },
                {
                  name: "photos",
                  list: "/photos",
                  show: "/photos/show/:id",
                  meta: {
                    icon: <PictureOutlined />,
                  },
                },
                {
                  name: "exports",
                  list: "/exports",
                  show: "/exports/show/:id",
                  create: "/exports/create",
                  meta: {
                    label: "Export Jobs",
                    icon: <FileZipOutlined />,
                  },
                },
              ]}
              options={{
                syncWithLocation: true,
                warnWhenUnsavedChanges: true,
              }}
            >
            <Routes>
              <Route
                element={
                  <ThemedLayoutV2
                    Header={() => <Header />}
                    Footer={() => <Footer />}
                    Sider={() => <ThemedSiderV2 Title={() => <Title />} />}
                  >
                    <Outlet />
                  </ThemedLayoutV2>
                }
              >
                <Route index element={<DashboardPage />} />
                <Route path="/users">
                  <Route index element={<UserList />} />
                  <Route path="show/:id" element={<UserShow />} />
                  <Route path="edit/:id" element={<UserEdit />} />
                </Route>
                <Route path="/trips">
                  <Route index element={<TripList />} />
                  <Route path="show/:id" element={<TripShow />} />
                  <Route path="edit/:id" element={<TripEdit />} />
                  <Route path="create" element={<TripCreate />} />
                </Route>
                <Route path="/place-visits">
                  <Route index element={<PlaceVisitList />} />
                  <Route path="show/:id" element={<PlaceVisitShow />} />
                  <Route path="edit/:id" element={<PlaceVisitEdit />} />
                </Route>
                <Route path="/locations">
                  <Route index element={<LocationList />} />
                  <Route path="show/:id" element={<LocationShow />} />
                </Route>
                <Route path="/photos">
                  <Route index element={<PhotoList />} />
                  <Route path="show/:id" element={<PhotoShow />} />
                </Route>
                <Route path="/exports">
                  <Route index element={<ExportList />} />
                  <Route path="show/:id" element={<ExportShow />} />
                  <Route path="create" element={<ExportCreate />} />
                </Route>
                <Route path="*" element={<ErrorComponent />} />
              </Route>

              <Route
                element={
                  <CatchAllNavigate to="/" />
                }
              >
                <Route
                  path="/login"
                  element={
                    <AuthPage
                      type="login"
                      formProps={{
                        initialValues: {
                          email: "admin@trailglass.local",
                          password: "",
                        },
                      }}
                    />
                  }
                />
              </Route>
            </Routes>
            <RefineKbar />
            <UnsavedChangesNotifier />
            <DocumentTitleHandler />
          </Refine>
        </AntdApp>
        </ConfigProvider>
      </RefineKbarProvider>
    </BrowserRouter>
  );
}

export default App;
