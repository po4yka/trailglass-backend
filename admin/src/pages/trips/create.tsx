import { Create, useForm } from "@refinedev/antd";
import { Form, Input, DatePicker } from "antd";
import dayjs from "dayjs";

export const TripCreate: React.FC = () => {
  const { formProps, saveButtonProps } = useForm({
    transform: (data) => {
      return {
        ...data,
        startDate: data.startDate ? dayjs(data.startDate).toISOString() : undefined,
        endDate: data.endDate ? dayjs(data.endDate).toISOString() : undefined,
      };
    },
  });

  return (
    <Create saveButtonProps={saveButtonProps}>
      <Form {...formProps} layout="vertical">
        <Form.Item
          label="Name"
          name="name"
          rules={[
            {
              required: true,
            },
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="Start Date"
          name="startDate"
          rules={[
            {
              required: true,
            },
          ]}
        >
          <DatePicker style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item
          label="End Date"
          name="endDate"
        >
          <DatePicker style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item label="Notes" name="notes">
          <Input.TextArea rows={4} />
        </Form.Item>
      </Form>
    </Create>
  );
};
