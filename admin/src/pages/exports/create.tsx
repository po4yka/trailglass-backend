import { Create, useForm } from "@refinedev/antd";
import { Form, DatePicker, Select } from "antd";
import dayjs from "dayjs";

const { RangePicker } = DatePicker;

export const ExportCreate: React.FC = () => {
  const { formProps, saveButtonProps } = useForm();

  const exportFormats = [
    { value: "json", label: "JSON" },
    { value: "csv", label: "CSV" },
    { value: "xlsx", label: "Excel (XLSX)" },
    { value: "pdf", label: "PDF" },
  ];

  return (
    <Create saveButtonProps={saveButtonProps}>
      <Form {...formProps} layout="vertical">
        <Form.Item
          label="Export Format"
          name="format"
          rules={[
            {
              required: true,
              message: "Please select an export format",
            },
          ]}
          initialValue="json"
        >
          <Select
            placeholder="Select export format"
            options={exportFormats}
          />
        </Form.Item>

        <Form.Item
          label="Date Range"
          name="dateRange"
          rules={[
            {
              required: true,
              message: "Please select a date range",
            },
          ]}
          initialValue={[dayjs().subtract(30, "days"), dayjs()]}
        >
          <RangePicker
            style={{ width: "100%" }}
            format="YYYY-MM-DD"
            presets={[
              { label: "Last 7 Days", value: [dayjs().subtract(7, "days"), dayjs()] },
              { label: "Last 30 Days", value: [dayjs().subtract(30, "days"), dayjs()] },
              { label: "Last 90 Days", value: [dayjs().subtract(90, "days"), dayjs()] },
              { label: "This Year", value: [dayjs().startOf("year"), dayjs()] },
              { label: "All Time", value: [dayjs("2020-01-01"), dayjs()] },
            ]}
          />
        </Form.Item>

        <Form.Item
          label="Export Type"
          name="exportType"
          rules={[
            {
              required: true,
              message: "Please select what to export",
            },
          ]}
          initialValue="all"
        >
          <Select
            placeholder="Select data to export"
            options={[
              { value: "all", label: "All Data" },
              { value: "trips", label: "Trips Only" },
              { value: "locations", label: "Locations Only" },
              { value: "place-visits", label: "Place Visits Only" },
              { value: "users", label: "Users Only" },
            ]}
          />
        </Form.Item>
      </Form>
    </Create>
  );
};
