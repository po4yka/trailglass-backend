import { DataProvider } from "@refinedev/core";
import { stringify } from "query-string";

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const getHeaders = () => {
  const token = localStorage.getItem("accessToken");
  return {
    "Content-Type": "application/json",
    "Authorization": token ? `Bearer ${token}` : "",
    "X-Device-ID": "admin-panel",
    "X-App-Version": "1.0.0",
  };
};

export const dataProvider: DataProvider = {
  getList: async ({ resource, pagination, filters, sorters }) => {
    const url = `${API_URL}/${resource}`;

    const params: any = {};

    if (pagination) {
      params.limit = pagination.pageSize;
      params.offset = (pagination.current - 1) * pagination.pageSize;
    }

    if (sorters && sorters.length > 0) {
      params.sort = sorters[0].field;
      params.order = sorters[0].order;
    }

    if (filters) {
      filters.forEach((filter) => {
        if ("field" in filter) {
          params[filter.field] = filter.value;
        }
      });
    }

    const queryString = stringify(params);
    const fullUrl = queryString ? `${url}?${queryString}` : url;

    const response = await fetch(fullUrl, {
      headers: getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    return {
      data: data[resource] || data,
      total: data.pagination?.total || data[resource]?.length || 0,
    };
  },

  getOne: async ({ resource, id }) => {
    const url = `${API_URL}/${resource}/${id}`;

    const response = await fetch(url, {
      headers: getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    return {
      data,
    };
  },

  create: async ({ resource, variables }) => {
    const url = `${API_URL}/${resource}`;

    const response = await fetch(url, {
      method: "POST",
      headers: getHeaders(),
      body: JSON.stringify(variables),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    return {
      data,
    };
  },

  update: async ({ resource, id, variables }) => {
    const url = `${API_URL}/${resource}/${id}`;

    const response = await fetch(url, {
      method: "PUT",
      headers: getHeaders(),
      body: JSON.stringify(variables),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    return {
      data,
    };
  },

  deleteOne: async ({ resource, id }) => {
    const url = `${API_URL}/${resource}/${id}`;

    const response = await fetch(url, {
      method: "DELETE",
      headers: getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return {
      data: { id },
    };
  },

  getApiUrl: () => API_URL,

  custom: async ({ url, method, filters, sorters, payload, query, headers }) => {
    let requestUrl = `${url}?`;

    if (sorters && sorters.length > 0) {
      const sortQuery = {
        sort: sorters[0].field,
        order: sorters[0].order,
      };
      requestUrl = `${requestUrl}&${stringify(sortQuery)}`;
    }

    if (filters) {
      const filterQuery = filters.reduce((acc, filter) => {
        if ("field" in filter) {
          acc[filter.field] = filter.value;
        }
        return acc;
      }, {} as any);
      requestUrl = `${requestUrl}&${stringify(filterQuery)}`;
    }

    if (query) {
      requestUrl = `${requestUrl}&${stringify(query)}`;
    }

    const response = await fetch(requestUrl, {
      method,
      headers: {
        ...getHeaders(),
        ...headers,
      },
      body: payload ? JSON.stringify(payload) : undefined,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    return {
      data,
    };
  },
};
