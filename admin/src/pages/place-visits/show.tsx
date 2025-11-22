import { Show, TextField, DateField, BooleanField, NumberField } from "@refinedev/antd";
import { Typography } from "antd";
import { useShow } from "@refinedev/core";

const { Title } = Typography;

export const PlaceVisitShow: React.FC = () => {
  const { queryResult } = useShow();
  const { data, isLoading } = queryResult;
  const record = data?.data;

  return (
    <Show isLoading={isLoading}>
      <Title level={5}>ID</Title>
      <TextField value={record?.id} />

      <Title level={5}>Place Name</Title>
      <TextField value={record?.placeName} />

      <Title level={5}>Address</Title>
      <TextField value={record?.address || "N/A"} />

      <Title level={5}>Category</Title>
      <TextField value={record?.category} />

      <Title level={5}>Arrival Time</Title>
      <DateField value={record?.arrivalTime} format="LLL" />

      <Title level={5}>Departure Time</Title>
      {record?.departureTime ? (
        <DateField value={record.departureTime} format="LLL" />
      ) : (
        <TextField value="N/A" />
      )}

      <Title level={5}>Duration (minutes)</Title>
      <NumberField value={record?.durationMinutes || 0} />

      <Title level={5}>Confidence</Title>
      <NumberField value={record?.confidence || 0} suffix="%" />

      <Title level={5}>Is Favorite</Title>
      <BooleanField value={record?.isFavorite} />

      <Title level={5}>Notes</Title>
      <TextField value={record?.notes || "N/A"} />

      <Title level={5}>Location</Title>
      <TextField
        value={
          record?.location
            ? `${record.location.latitude}, ${record.location.longitude}`
            : "N/A"
        }
      />
    </Show>
  );
};
