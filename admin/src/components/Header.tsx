import { Layout, Avatar, Dropdown, Space, Typography } from "antd";
import { UserOutlined, LogoutOutlined, SettingOutlined, CompassOutlined } from "@ant-design/icons";
import { useLogout, useGetIdentity } from "@refinedev/core";
import "../App.css";

const { Header: AntdHeader } = Layout;
const { Text } = Typography;

export const Header: React.FC = () => {
  const { mutate: logout } = useLogout();
  const { data: user } = useGetIdentity<{ email: string }>();

  const menuItems = [
    {
      key: "profile",
      icon: <UserOutlined />,
      label: "Profile",
    },
    {
      key: "settings",
      icon: <SettingOutlined />,
      label: "Settings",
    },
    {
      type: "divider" as const,
    },
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "Logout",
      danger: true,
      onClick: () => logout(),
    },
  ];

  return (
    <AntdHeader className="app-header">
      <div className="app-header-left">
        <div className="app-logo">
          <CompassOutlined />
        </div>
        <h1 className="app-title">Trailglass Admin</h1>
      </div>
      <div className="app-header-right">
        <Dropdown menu={{ items: menuItems }} placement="bottomRight">
          <Space style={{ cursor: "pointer" }}>
            <Avatar icon={<UserOutlined />} style={{ backgroundColor: "#0891b2" }} />
            <Text style={{ color: "white" }}>{user?.email || "Admin"}</Text>
          </Space>
        </Dropdown>
      </div>
    </AntdHeader>
  );
};
