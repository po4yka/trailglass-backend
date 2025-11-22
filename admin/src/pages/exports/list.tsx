import { List, useTable, DateField, ShowButton } from "@refinedev/antd";
import { Table, Space, Tag, Progress, Button, Typography } from "antd";
import { DownloadOutlined } from "@ant-design/icons";

const { Text } = Typography;

interface ExportJob {
  id: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  format: string;
  progress: number;
  fileSize?: number;
  downloadUrl?: string;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
  startDate?: string;
  endDate?: string;
  error?: string;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "COMPLETED":
      return "success";
    case "PROCESSING":
      return "processing";
    case "PENDING":
      return "default";
    case "FAILED":
      return "error";
    default:
      return "default";
  }
};

const formatFileSize = (bytes?: number): string => {
  if (!bytes) return "N/A";
  const units = ["B", "KB", "MB", "GB"];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(2)} ${units[unitIndex]}`;
};

export const ExportList: React.FC = () => {
  const { tableProps } = useTable({
    resource: "exports",
  });

  return (
    <List>
      <Table {...tableProps} rowKey="id">
        <Table.Column dataIndex="id" title="ID" width={100} />
        <Table.Column
          dataIndex="status"
          title="Status"
          width={120}
          render={(status: string) => (
            <Tag color={getStatusColor(status)}>{status}</Tag>
          )}
        />
        <Table.Column
          dataIndex="format"
          title="Format"
          width={80}
          render={(format: string) => format?.toUpperCase() || "N/A"}
        />
        <Table.Column
          dataIndex="progress"
          title="Progress"
          width={150}
          render={(progress: number, record: ExportJob) => {
            if (record.status === "COMPLETED") {
              return <Progress percent={100} size="small" status="success" />;
            }
            if (record.status === "PROCESSING") {
              return (
                <Progress
                  percent={progress || 0}
                  size="small"
                  status="active"
                />
              );
            }
            if (record.status === "FAILED") {
              return <Progress percent={0} size="small" status="exception" />;
            }
            return <Progress percent={0} size="small" />;
          }}
        />
        <Table.Column
          dataIndex="fileSize"
          title="File Size"
          width={100}
          render={(size: number) => <Text>{formatFileSize(size)}</Text>}
        />
        <Table.Column
          dataIndex="expiresAt"
          title="Expires At"
          width={180}
          render={(value) =>
            value ? <DateField value={value} format="LLL" /> : <Text>N/A</Text>
          }
        />
        <Table.Column
          dataIndex="createdAt"
          title="Created At"
          width={180}
          render={(value) => <DateField value={value} format="LLL" />}
        />
        <Table.Column
          title="Actions"
          dataIndex="actions"
          width={150}
          render={(_, record: ExportJob) => (
            <Space>
              <ShowButton hideText size="small" recordItemId={record.id} />
              {record.status === "COMPLETED" && record.downloadUrl && (
                <Button
                  type="primary"
                  size="small"
                  icon={<DownloadOutlined />}
                  href={record.downloadUrl}
                  target="_blank"
                >
                  Download
                </Button>
              )}
            </Space>
          )}
        />
      </Table>
    </List>
  );
};
