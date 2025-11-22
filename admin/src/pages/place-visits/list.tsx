import { List, useTable, DateField, ShowButton, EditButton } from "@refinedev/antd";
import { Table, Space, Tag } from "antd";

export const PlaceVisitList: React.FC = () => {
  const { tableProps } = useTable({
    resource: "place-visits",
  });

  return (
    <List>
      <Table {...tableProps} rowKey="id">
        <Table.Column dataIndex="id" title="ID" width={80} />
        <Table.Column dataIndex="placeName" title="Place Name" />
        <Table.Column dataIndex="category" title="Category"
          render={(value) => <Tag>{value}</Tag>}
        />
        <Table.Column
          dataIndex="arrivalTime"
          title="Arrival"
          render={(value) => <DateField value={value} format="LLL" />}
        />
        <Table.Column
          dataIndex="departureTime"
          title="Departure"
          render={(value) => value ? <DateField value={value} format="LLL" /> : "N/A"}
        />
        <Table.Column
          dataIndex="isFavorite"
          title="Favorite"
          render={(value) => value ? <Tag color="gold">Yes</Tag> : <Tag>No</Tag>}
        />
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
