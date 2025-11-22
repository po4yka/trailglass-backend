import { Edit, useForm } from "@refinedev/antd";
import { Form, Input, Switch, Select } from "antd";

export const PlaceVisitEdit: React.FC = () => {
  const { formProps, saveButtonProps } = useForm();

  return (
    <Edit saveButtonProps={saveButtonProps}>
      <Form {...formProps} layout="vertical">
        <Form.Item
          label="Place Name"
          name="placeName"
          rules={[
            {
              required: true,
            },
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item label="Address" name="address">
          <Input />
        </Form.Item>
        <Form.Item label="Category" name="category">
          <Select>
            <Select.Option value="OUTDOOR">Outdoor</Select.Option>
            <Select.Option value="EDUCATION">Education</Select.Option>
            <Select.Option value="RESTAURANT">Restaurant</Select.Option>
            <Select.Option value="SHOPPING">Shopping</Select.Option>
            <Select.Option value="ENTERTAINMENT">Entertainment</Select.Option>
            <Select.Option value="TRANSPORT">Transport</Select.Option>
            <Select.Option value="ACCOMMODATION">Accommodation</Select.Option>
            <Select.Option value="OTHER">Other</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="Notes" name="notes">
          <Input.TextArea rows={4} />
        </Form.Item>
        <Form.Item
          label="Is Favorite"
          name="isFavorite"
          valuePropName="checked"
        >
          <Switch />
        </Form.Item>
      </Form>
    </Edit>
  );
};
