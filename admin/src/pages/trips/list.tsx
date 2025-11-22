import { List, useTable, DateField, ShowButton, EditButton } from "@refinedev/antd";
import { Table, Space } from "antd";

export const TripList: React.FC = () => {
  const { tableProps } = useTable({
    resource: "trips",
  });

  return (
    <List>
      <Table {...tableProps} rowKey="id">
        <Table.Column dataIndex="id" title="ID" />
        <Table.Column dataIndex="name" title="Name" />
        <Table.Column
          dataIndex="startDate"
          title="Start Date"
          render={(value) => <DateField value={value} format="LL" />}
        />
        <Table.Column
          dataIndex="endDate"
          title="End Date"
          render={(value) => <DateField value={value} format="LL" />}
        />
        <Table.Column dataIndex="totalDistance" title="Distance (km)" />
        <Table.Column
          title="Actions"
          dataIndex="actions"
          render={(_, record: any) => (
            <Space>
              <ShowButton hideText size="small" recordItemId={record.id} />
              <EditButton hideText size="small" recordItemId={record.id} />
            </Space>
          )}
        />
      </Table>
    </List>
  );
};
