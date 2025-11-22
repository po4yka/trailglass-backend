import { Show, TextField, DateField } from "@refinedev/antd";
import { Typography, Tag, Progress, Button, Alert, Descriptions, Badge } from "antd";
import { useShow } from "@refinedev/core";
import { DownloadOutlined } from "@ant-design/icons";

const { Title } = Typography;

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

export const ExportShow: React.FC = () => {
  const { queryResult } = useShow();
  const { data, isLoading } = queryResult;
  const record = data?.data as ExportJob | undefined;

  return (
    <Show isLoading={isLoading}>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="ID">
          <TextField value={record?.id} />
        </Descriptions.Item>

        <Descriptions.Item label="Status">
          <Badge
            status={
              record?.status === "COMPLETED"
                ? "success"
                : record?.status === "PROCESSING"
                ? "processing"
                : record?.status === "FAILED"
                ? "error"
                : "default"
            }
            text={
              <Tag color={getStatusColor(record?.status || "")}>
                {record?.status}
              </Tag>
            }
          />
        </Descriptions.Item>

        <Descriptions.Item label="Format">
          <TextField value={record?.format?.toUpperCase() || "N/A"} />
        </Descriptions.Item>

        <Descriptions.Item label="Progress">
          {record?.status === "COMPLETED" && (
            <Progress percent={100} status="success" />
          )}
          {record?.status === "PROCESSING" && (
            <Progress percent={record?.progress || 0} status="active" />
          )}
          {record?.status === "FAILED" && (
            <Progress percent={0} status="exception" />
          )}
          {record?.status === "PENDING" && <Progress percent={0} />}
        </Descriptions.Item>

        <Descriptions.Item label="File Size">
          <TextField value={formatFileSize(record?.fileSize)} />
        </Descriptions.Item>

        <Descriptions.Item label="Date Range">
          {record?.startDate && record?.endDate ? (
            <>
              <DateField value={record.startDate} format="LL" /> -{" "}
              <DateField value={record.endDate} format="LL" />
            </>
          ) : (
            <TextField value="N/A" />
          )}
        </Descriptions.Item>

        <Descriptions.Item label="Created At">
          <DateField value={record?.createdAt} format="LLL" />
        </Descriptions.Item>

        <Descriptions.Item label="Updated At">
          <DateField value={record?.updatedAt} format="LLL" />
        </Descriptions.Item>

        <Descriptions.Item label="Expires At">
          {record?.expiresAt ? (
            <DateField value={record.expiresAt} format="LLL" />
          ) : (
            <TextField value="N/A" />
          )}
        </Descriptions.Item>

        {record?.downloadUrl && record?.status === "COMPLETED" && (
          <Descriptions.Item label="Download">
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              href={record.downloadUrl}
              target="_blank"
            >
              Download Export File
            </Button>
          </Descriptions.Item>
        )}

        {record?.error && record?.status === "FAILED" && (
          <Descriptions.Item label="Error">
            <Alert
              message="Export Failed"
              description={record.error}
              type="error"
              showIcon
            />
          </Descriptions.Item>
        )}
      </Descriptions>
    </Show>
  );
};
