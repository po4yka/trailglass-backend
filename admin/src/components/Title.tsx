import { CompassOutlined } from "@ant-design/icons";
import "../App.css";

export const Title: React.FC = () => {
  return (
    <div className="sider-title">
      <div className="sider-logo">
        <CompassOutlined />
      </div>
      <h2 className="sider-title-text">Trailglass</h2>
    </div>
  );
};
