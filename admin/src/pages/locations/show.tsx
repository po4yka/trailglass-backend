import { Show, TextField, DateField, NumberField } from "@refinedev/antd";
import { Typography } from "antd";
import { useShow } from "@refinedev/core";

const { Title } = Typography;

export const LocationShow: React.FC = () => {
  const { queryResult } = useShow();
  const { data, isLoading } = queryResult;
  const record = data?.data;

  return (
    <Show isLoading={isLoading}>
      <Title level={5}>ID</Title>
      <TextField value={record?.id} />

      <Title level={5}>Timestamp</Title>
      <DateField value={record?.timestamp} format="LLL" />

      <Title level={5}>Latitude</Title>
      <NumberField value={record?.latitude} options={{ maximumFractionDigits: 8 }} />

      <Title level={5}>Longitude</Title>
      <NumberField value={record?.longitude} options={{ maximumFractionDigits: 8 }} />

      <Title level={5}>Altitude</Title>
      {record?.altitude ? (
        <NumberField value={record.altitude} suffix=" m" />
      ) : (
        <TextField value="N/A" />
      )}

      <Title level={5}>Accuracy</Title>
      <NumberField value={record?.accuracy} suffix=" m" />

      <Title level={5}>Speed</Title>
      {record?.speed ? (
        <NumberField value={record.speed} suffix=" m/s" />
      ) : (
        <TextField value="N/A" />
      )}

      <Title level={5}>Bearing</Title>
      {record?.bearing ? (
        <NumberField value={record.bearing} suffix="°" />
      ) : (
        <TextField value="N/A" />
      )}

      <Title level={5}>Provider</Title>
      <TextField value={record?.provider} />

      <Title level={5}>Battery Level</Title>
      {record?.batteryLevel ? (
        <NumberField
          value={record.batteryLevel * 100}
          suffix="%"
          options={{ maximumFractionDigits: 0 }}
        />
      ) : (
        <TextField value="N/A" />
      )}
    </Show>
  );
};
