import styled from 'styled-components';
import { adminMenuItems } from '../../mocks/admin';
import AdminSidebar from './AdminSidebar';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 202px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Content = styled.main`
  min-width: 0;
  padding-top: ${({ $contentPaddingTop }) => ($contentPaddingTop ? '4px' : '0')};
`;

// AI작성: 관리자 화면의 동일한 제목·사이드바·본문 배치를 한 곳에서 관리한다.
function AdminPageLayout({
  title,
  description,
  activeItem,
  headingAddon = null,
  contentPaddingTop = false,
  children,
  overlay = null,
}) {
  return (
    <Page>
      <Heading>
        <div>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        {headingAddon}
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem={activeItem} />
        <Content $contentPaddingTop={contentPaddingTop}>{children}</Content>
      </Grid>
      {overlay}
    </Page>
  );
}

export default AdminPageLayout;
