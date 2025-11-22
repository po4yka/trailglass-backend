import { Show, TextField, DateField, NumberField } from "@refinedev/antd";
import { Typography, Image, Card, Row, Col, Descriptions } from "antd";
import { useShow } from "@refinedev/core";

const { Title } = Typography;

export const PhotoShow: React.FC = () => {
  const { queryResult } = useShow();
  const { data, isLoading } = queryResult;
  const record = data?.data;

  return (
    <Show isLoading={isLoading}>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Photo Preview">
            <Image
              width="100%"
              src={record?.download?.url}
              fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mN8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
            />
            {record?.thumbnailUrl?.url && (
              <div style={{ marginTop: 16 }}>
                <Title level={5}>Thumbnail</Title>
                <Image
                  width={150}
                  src={record.thumbnailUrl.url}
                  style={{ objectFit: "cover" }}
                />
              </div>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Photo Information">
            <Title level={5}>ID</Title>
            <TextField value={record?.id} />

            <Title level={5}>File Name</Title>
            <TextField value={record?.fileName} />

            <Title level={5}>MIME Type</Title>
            <TextField value={record?.mimeType} />

            <Title level={5}>Size</Title>
            <NumberField
              value={record?.sizeBytes}
              options={{
                notation: "compact",
                compactDisplay: "short",
              }}
            />
            <span> bytes</span>

            <Title level={5}>Caption</Title>
            <TextField value={record?.caption || "N/A"} />

            <Title level={5}>Timestamp</Title>
            {record?.timestamp ? (
              <DateField value={record.timestamp} format="LLLL" />
            ) : (
              <TextField value="N/A" />
            )}

            <Title level={5}>Uploaded At</Title>
            {record?.uploadedAt ? (
              <DateField value={record.uploadedAt} format="LLLL" />
            ) : (
              <TextField value="N/A" />
            )}

            <Title level={5}>Updated At</Title>
            <DateField value={record?.updatedAt} format="LLLL" />
          </Card>

          <Card title="Location Information" style={{ marginTop: 16 }}>
            <Title level={5}>Coordinates</Title>
            <TextField
              value={
                record?.location
                  ? `${record.location.latitude}, ${record.location.longitude}`
                  : "N/A"
              }
            />

            <Title level={5}>Place Visit ID</Title>
            <TextField value={record?.placeVisitId || "N/A"} />

            <Title level={5}>Trip ID</Title>
            <TextField value={record?.tripId || "N/A"} />
          </Card>

          {record?.exifData && (
            <Card title="EXIF Data" style={{ marginTop: 16 }}>
              <Descriptions column={1} bordered size="small">
                <Descriptions.Item label="Camera Model">
                  {record.exifData.cameraModel || "N/A"}
                </Descriptions.Item>
                <Descriptions.Item label="Focal Length">
                  {record.exifData.focalLength ? `${record.exifData.focalLength}mm` : "N/A"}
                </Descriptions.Item>
                <Descriptions.Item label="Aperture">
                  {record.exifData.aperture ? `f/${record.exifData.aperture}` : "N/A"}
                </Descriptions.Item>
                <Descriptions.Item label="ISO">
                  {record.exifData.iso || "N/A"}
                </Descriptions.Item>
                <Descriptions.Item label="Shutter Speed">
                  {record.exifData.shutterSpeed || "N/A"}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          <Card title="Storage Information" style={{ marginTop: 16 }}>
            <Title level={5}>Storage Key</Title>
            <TextField value={record?.storageKey} />

            <Title level={5}>Thumbnail Storage Key</Title>
            <TextField value={record?.thumbnailStorageKey || "N/A"} />

            <Title level={5}>Storage Backend</Title>
            <TextField value={record?.storageBackend} />

            <Title level={5}>Server Version</Title>
            <NumberField value={record?.serverVersion} />
          </Card>
        </Col>
      </Row>
    </Show>
  );
};
