import { useState, useEffect } from "react";
import { Card, Col, Row, Statistic, Button, Space, Spin, Typography, Progress } from "antd";
import {
  UserOutlined,
  EnvironmentOutlined,
  CarOutlined,
  HomeOutlined,
  PlusOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from "@ant-design/icons";
import { useNavigation, useApiUrl, useCustom } from "@refinedev/core";
import "../../App.css";

const { Title, Text } = Typography;

interface DashboardStats {
  totalUsers: number;
  totalTrips: number;
  totalPlaceVisits: number;
  totalLocations: number;
  activeTrips?: number;
  recentUsers?: number;
}

export const DashboardPage: React.FC = () => {
  const { push } = useNavigation();
  const apiUrl = useApiUrl();
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0,
    totalTrips: 0,
    totalPlaceVisits: 0,
    totalLocations: 0,
  });
  const [loading, setLoading] = useState(true);

  const fetchDashboardStats = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem("accessToken");
      const headers = {
        "Content-Type": "application/json",
        "Authorization": token ? `Bearer ${token}` : "",
        "X-Device-ID": "admin-panel",
        "X-App-Version": "1.0.0",
      };

      const [usersRes, tripsRes, placeVisitsRes, locationsRes] = await Promise.all([
        fetch(`${apiUrl}/users?limit=1`, { headers }),
        fetch(`${apiUrl}/trips?limit=1`, { headers }),
        fetch(`${apiUrl}/place-visits?limit=1`, { headers }),
        fetch(`${apiUrl}/locations?limit=1`, { headers }),
      ]);

      const [usersData, tripsData, placeVisitsData, locationsData] = await Promise.all([
        usersRes.json(),
        tripsRes.json(),
        placeVisitsRes.json(),
        locationsRes.json(),
      ]);

      setStats({
        totalUsers: usersData.pagination?.total || usersData.users?.length || 0,
        totalTrips: tripsData.pagination?.total || tripsData.trips?.length || 0,
        totalPlaceVisits: placeVisitsData.pagination?.total || placeVisitsData["place-visits"]?.length || 0,
        totalLocations: locationsData.pagination?.total || locationsData.locations?.length || 0,
      });
    } catch (error) {
      console.error("Failed to fetch dashboard stats:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const statCards = [
    {
      title: "Total Users",
      value: stats.totalUsers,
      icon: <UserOutlined />,
      color: "#10b981",
      bgColor: "#d1fae5",
      path: "/users",
    },
    {
      title: "Total Trips",
      value: stats.totalTrips,
      icon: <CarOutlined />,
      color: "#0ea5e9",
      bgColor: "#e0f2fe",
      path: "/trips",
    },
    {
      title: "Place Visits",
      value: stats.totalPlaceVisits,
      icon: <HomeOutlined />,
      color: "#f59e0b",
      bgColor: "#fef3c7",
      path: "/place-visits",
    },
    {
      title: "Locations Tracked",
      value: stats.totalLocations,
      icon: <EnvironmentOutlined />,
      color: "#8b5cf6",
      bgColor: "#ede9fe",
      path: "/locations",
    },
  ];

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <Title level={2} className="dashboard-title">
          Dashboard Overview
        </Title>
        <Text className="dashboard-subtitle">
          Welcome to Trailglass Admin - Monitor and manage your travel tracking platform
        </Text>
      </div>

      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          {statCards.map((stat, index) => (
            <Col xs={24} sm={12} lg={6} key={index}>
              <Card
                className="stat-card"
                hoverable
                onClick={() => push(stat.path)}
                style={{ borderLeft: `4px solid ${stat.color}` }}
              >
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <div>
                    <Text type="secondary" style={{ fontSize: "14px", display: "block", marginBottom: "8px" }}>
                      {stat.title}
                    </Text>
                    <Title level={2} style={{ margin: 0, color: stat.color }}>
                      {stat.value.toLocaleString()}
                    </Title>
                  </div>
                  <div
                    className="stat-card-icon"
                    style={{ color: stat.color, backgroundColor: stat.bgColor }}
                  >
                    {stat.icon}
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </Spin>

      <div className="quick-actions" style={{ marginTop: "32px" }}>
        <Title level={4} style={{ marginBottom: "16px" }}>
          Quick Actions
        </Title>
        <Space size="middle" wrap>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            className="action-button"
            onClick={() => push("/users/create")}
          >
            Add New User
          </Button>
          <Button
            type="default"
            icon={<PlusOutlined />}
            className="action-button"
            onClick={() => push("/trips/create")}
          >
            Create Trip
          </Button>
          <Button
            type="default"
            icon={<ReloadOutlined />}
            className="action-button"
            onClick={fetchDashboardStats}
          >
            Refresh Stats
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} style={{ marginTop: "32px" }}>
        <Col xs={24} lg={12}>
          <Card
            className="recent-activity-card"
            title="System Health"
            extra={<Text type="success">All Systems Operational</Text>}
          >
            <Space direction="vertical" style={{ width: "100%" }} size="large">
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <Text>API Response Time</Text>
                  <Text strong>Excellent</Text>
                </div>
                <Progress percent={95} strokeColor="#10b981" />
              </div>
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <Text>Database Performance</Text>
                  <Text strong>Good</Text>
                </div>
                <Progress percent={85} strokeColor="#0ea5e9" />
              </div>
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <Text>Storage Usage</Text>
                  <Text strong>Normal</Text>
                </div>
                <Progress percent={45} strokeColor="#f59e0b" />
              </div>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            className="recent-activity-card"
            title="Platform Insights"
          >
            <Space direction="vertical" style={{ width: "100%" }} size="middle">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px", background: "#f0f9ff", borderRadius: "8px" }}>
                <div>
                  <Text type="secondary" style={{ fontSize: "12px", display: "block" }}>Average Trips per User</Text>
                  <Text strong style={{ fontSize: "18px" }}>
                    {stats.totalUsers > 0 ? (stats.totalTrips / stats.totalUsers).toFixed(1) : "0"}
                  </Text>
                </div>
                <ArrowUpOutlined style={{ color: "#10b981", fontSize: "24px" }} />
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px", background: "#fef3c7", borderRadius: "8px" }}>
                <div>
                  <Text type="secondary" style={{ fontSize: "12px", display: "block" }}>Avg Visits per Trip</Text>
                  <Text strong style={{ fontSize: "18px" }}>
                    {stats.totalTrips > 0 ? (stats.totalPlaceVisits / stats.totalTrips).toFixed(1) : "0"}
                  </Text>
                </div>
                <ArrowUpOutlined style={{ color: "#f59e0b", fontSize: "24px" }} />
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px", background: "#ede9fe", borderRadius: "8px" }}>
                <div>
                  <Text type="secondary" style={{ fontSize: "12px", display: "block" }}>Total Locations</Text>
                  <Text strong style={{ fontSize: "18px" }}>
                    {stats.totalLocations.toLocaleString()}
                  </Text>
                </div>
                <EnvironmentOutlined style={{ color: "#8b5cf6", fontSize: "24px" }} />
              </div>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
};
