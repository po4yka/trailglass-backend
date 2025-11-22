import { List, useTable, DateField, ShowButton, NumberField } from "@refinedev/antd";
import { Table, Space } from "antd";

export const LocationList: React.FC = () => {
  const { tableProps } = useTable({
    resource: "locations",
    pagination: {
      pageSize: 50,
    },
  });

  return (
    <List>
      <Table {...tableProps} rowKey="id" scroll={{ x: 1200 }}>
        <Table.Column dataIndex="id" title="ID" width={80} />
        <Table.Column
          dataIndex="latitude"
          title="Latitude"
          render={(value) => <NumberField value={value} options={{ maximumFractionDigits: 6 }} />}
        />
        <Table.Column
          dataIndex="longitude"
          title="Longitude"
          render={(value) => <NumberField value={value} options={{ maximumFractionDigits: 6 }} />}
        />
        <Table.Column
          dataIndex="altitude"
          title="Altitude"
          render={(value) => value ? <NumberField value={value} suffix=" m" /> : "N/A"}
        />
        <Table.Column
          dataIndex="accuracy"
          title="Accuracy"
          render={(value) => <NumberField value={value} suffix=" m" />}
        />
        <Table.Column
          dataIndex="timestamp"
          title="Timestamp"
          render={(value) => <DateField value={value} format="LLL" />}
        />
        <Table.Column dataIndex="provider" title="Provider" />
        <Table.Column
          title="Actions"
          dataIndex="actions"
          render={(_, record: any) => (
            <Space>
              <ShowButton hideText size="small" recordItemId={record.id} />
            </Space>
          )}
        />
      </Table>
    </List>
  );
};
