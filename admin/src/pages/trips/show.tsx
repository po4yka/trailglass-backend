import { Show, TextField, DateField, NumberField } from "@refinedev/antd";
import { Typography } from "antd";
import { useShow } from "@refinedev/core";

const { Title } = Typography;

export const TripShow: React.FC = () => {
  const { queryResult } = useShow();
  const { data, isLoading } = queryResult;
  const record = data?.data;

  return (
    <Show isLoading={isLoading}>
      <Title level={5}>ID</Title>
      <TextField value={record?.id} />

      <Title level={5}>Name</Title>
      <TextField value={record?.name} />

      <Title level={5}>Start Date</Title>
      <DateField value={record?.startDate} format="LLL" />

      <Title level={5}>End Date</Title>
      <DateField value={record?.endDate} format="LLL" />

      <Title level={5}>Total Distance</Title>
      <NumberField value={record?.totalDistance} suffix=" km" />

      <Title level={5}>Countries</Title>
      <TextField value={record?.countries?.join(", ") || "N/A"} />

      <Title level={5}>Notes</Title>
      <TextField value={record?.notes || "N/A"} />
    </Show>
  );
};
