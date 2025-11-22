import { List, useTable, DateField, ShowButton, TextField } from "@refinedev/antd";
import { Table, Space, Image, Tag, Select } from "antd";
import { useState } from "react";

export const PhotoList: React.FC = () => {
  const [filterPlaceVisit, setFilterPlaceVisit] = useState<string | undefined>();
  const [filterTrip, setFilterTrip] = useState<string | undefined>();

  const { tableProps } = useTable({
    resource: "photos",
    filters: {
      permanent: [
        ...(filterPlaceVisit ? [{ field: "placeVisitId", operator: "eq", value: filterPlaceVisit }] : []),
        ...(filterTrip ? [{ field: "tripId", operator: "eq", value: filterTrip }] : []),
      ],
    },
  });

  return (
    <List>
      <Space style={{ marginBottom: 16 }}>
        <span>Filter by Place Visit:</span>
        <Select
          allowClear
          placeholder="Select Place Visit"
          style={{ width: 200 }}
          onChange={(value) => setFilterPlaceVisit(value)}
          value={filterPlaceVisit}
        >
          {/* Place visit options would be loaded dynamically */}
        </Select>
        <span>Filter by Trip:</span>
        <Select
          allowClear
          placeholder="Select Trip"
          style={{ width: 200 }}
          onChange={(value) => setFilterTrip(value)}
          value={filterTrip}
        >
          {/* Trip options would be loaded dynamically */}
        </Select>
      </Space>
      <Table {...tableProps} rowKey="id">
        <Table.Column
          dataIndex="thumbnailUrl"
          title="Thumbnail"
          render={(value, record: any) => (
            <Image
              width={80}
              height={80}
              src={record?.thumbnailUrl?.url || record?.download?.url}
              fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mN8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
              preview={{
                src: record?.download?.url,
              }}
              style={{ objectFit: "cover" }}
            />
          )}
        />
        <Table.Column dataIndex="id" title="ID" width={100} ellipsis />
        <Table.Column
          dataIndex="fileName"
          title="File Name"
          render={(value) => <TextField value={value} ellipsis />}
        />
        <Table.Column
          dataIndex="caption"
          title="Caption"
          render={(value) => <TextField value={value || "N/A"} />}
        />
        <Table.Column
          dataIndex="timestamp"
          title="Timestamp"
          render={(value) => value ? <DateField value={value} format="LLL" /> : <TextField value="N/A" />}
        />
        <Table.Column
          dataIndex="location"
          title="Location"
          render={(value) => (
            value ? (
              <span>{value.latitude.toFixed(4)}, {value.longitude.toFixed(4)}</span>
            ) : (
              <TextField value="N/A" />
            )
          )}
        />
        <Table.Column
          dataIndex="placeVisitId"
          title="Place Visit"
          render={(value) => value ? <Tag>{value.substring(0, 8)}</Tag> : <TextField value="N/A" />}
        />
        <Table.Column
          dataIndex="tripId"
          title="Trip"
          render={(value) => value ? <Tag color="blue">{value.substring(0, 8)}</Tag> : <TextField value="N/A" />}
        />
        <Table.Column
          dataIndex="uploadedAt"
          title="Uploaded"
          render={(value) => value ? <DateField value={value} format="LLL" /> : <TextField value="N/A" />}
        />
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
