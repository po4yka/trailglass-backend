import { Show, TextField, DateField } from "@refinedev/antd";
import { Typography } from "antd";
import { useShow } from "@refinedev/core";

const { Title } = Typography;

export const UserShow: React.FC = () => {
  const { queryResult } = useShow();
  const { data, isLoading } = queryResult;
  const record = data?.data;

  return (
    <Show isLoading={isLoading}>
      <Title level={5}>ID</Title>
      <TextField value={record?.id} />

      <Title level={5}>Email</Title>
      <TextField value={record?.email} />

      <Title level={5}>Full Name</Title>
      <TextField value={record?.fullName} />

      <Title level={5}>Created At</Title>
      <DateField value={record?.createdAt} format="LLL" />

      <Title level={5}>Updated At</Title>
      <DateField value={record?.updatedAt} format="LLL" />
    </Show>
  );
};
