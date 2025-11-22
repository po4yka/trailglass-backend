import { Layout, Typography } from "antd";
import { CopyrightOutlined, HeartFilled } from "@ant-design/icons";
import "../App.css";

const { Footer: AntdFooter } = Layout;
const { Text } = Typography;

export const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();
  const version = "1.0.0";

  return (
    <AntdFooter className="app-footer">
      <div className="app-footer-content">
        <Text>
          <CopyrightOutlined /> {currentYear} Trailglass. Made with <HeartFilled style={{ color: "#ef4444" }} /> for travelers
        </Text>
        <Text type="secondary" style={{ marginLeft: "auto" }}>
          v{version}
        </Text>
      </div>
    </AntdFooter>
  );
};
