import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import FindAccountPage from './pages/FindAccountPage';
import MyPage from './pages/MyPage';
import EditProfilePage from './pages/EditProfilePage';
import DeleteAccountPage from './pages/DeleteAccountPage';
import VerificationPage from './pages/VerificationPage';
import NotificationPage from './pages/NotificationPage';
import PostListPage from './pages/PostListPage';
import PostCreatePage from './pages/PostCreatePage';
import PostDetailPage from './pages/PostDetailPage';
import JoinPage from './pages/JoinPage';
import AdminMembersPage from './pages/AdminMembersPage';
import AdminMemberDetailPage from './pages/AdminMemberDetailPage';
import AdminSavingGroupsPage from './pages/AdminSavingGroupsPage';
import AdminSavingGroupApplicationsPage from './pages/AdminSavingGroupApplicationsPage';
import AdminSavingGroupApplicationDetailPage from './pages/AdminSavingGroupApplicationDetailPage';
import AdminSmallGroupsPage from './pages/AdminSmallGroupsPage';
import AdminPostsPage from './pages/AdminPostsPage';
import AdminCommentsPage from './pages/AdminCommentsPage';
import AdminReportsPage from './pages/AdminReportsPage';
import AdminReportDetailPage from './pages/AdminReportDetailPage';
import SavingGroupsPage from './pages/SavingGroupsPage';
import SavingGroupDetailPage from './pages/SavingGroupDetailPage';
import SavingGroupApplicationPage from './pages/SavingGroupApplicationPage';
import SavingGroupSettingsPage from './pages/SavingGroupSettingsPage';
import SmallGroupsPage from './pages/SmallGroupsPage';
import SmallGroupCreatePage from './pages/SmallGroupCreatePage';
import SmallGroupDetailPage from './pages/SmallGroupDetailPage';
import SmallGroupVoteDetailPage from './pages/SmallGroupVoteDetailPage';
import NotFound from './pages/NotFound';
import SocialLoginCallbackPage from './pages/SocialLoginCallbackPage.jsx';
import ResetPasswordPage from './pages/ResetPasswordPage';

function App() {
  return (
    <Routes>
      {/* Layout 안의 <Outlet /> 자리에 아래 페이지들이 바뀌어 들어간다 */}
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/saving-groups" element={<SavingGroupsPage />} />
        <Route path="/saving-groups/new" element={<SavingGroupApplicationPage />} />
        <Route path="/saving-groups/:id/settings" element={<SavingGroupSettingsPage />} />
        <Route path="/saving-groups/:id" element={<SavingGroupDetailPage />} />
        <Route path="/small-groups/new" element={<SmallGroupCreatePage />} />
        <Route path="/small-groups/:smallGroupId/votes/:voteId" element={<SmallGroupVoteDetailPage />} />
        <Route path="/small-groups/:id" element={<SmallGroupDetailPage />} />
        <Route path="/small-groups" element={<SmallGroupsPage />} />
        <Route path="/member/login" element={<LoginPage />} />
        <Route path="/member/find-account" element={<FindAccountPage />} />
        <Route path="/member/reset-password" element={<ResetPasswordPage />} />

        {/* AI작성: 카카오 인증 후 로그인 결과를 처리하는 화면 */}
        <Route
          path="/member/social/callback"
          element={<SocialLoginCallbackPage />}
        />
        <Route path="/member/mypage" element={<MyPage />} />
        <Route path="/member/mypage/edit" element={<EditProfilePage />} />
        <Route path="/member/mypage/delete" element={<DeleteAccountPage />} />
        <Route path="/member/verify" element={<VerificationPage />} />
        <Route path="/member/notification" element={<NotificationPage />} />
        <Route path="/post/list" element={<PostListPage />} />
        <Route path="/post/create" element={<PostCreatePage />} />
        <Route path="/post/:id/edit" element={<PostCreatePage />} />
        <Route path="/post/:id" element={<PostDetailPage />} />
        <Route path="/admin/members" element={<AdminMembersPage />} />
        <Route path="/admin/members/:id" element={<AdminMemberDetailPage />} />
        <Route path="/admin/saving-groups" element={<AdminSavingGroupsPage />} />
        <Route path="/admin/saving-groups/applications" element={<AdminSavingGroupApplicationsPage />} />
        <Route path="/admin/saving-groups/applications/:id" element={<AdminSavingGroupApplicationDetailPage />} />
        <Route path="/admin/small-groups" element={<AdminSmallGroupsPage />} />
        <Route path="/admin/posts" element={<AdminPostsPage />} />
        <Route path="/admin/comments" element={<AdminCommentsPage />} />
        <Route path="/admin/reposts" element={<AdminReportsPage />} />
        <Route path="/admin/reposts/:id" element={<AdminReportDetailPage />} />
        <Route path="/member/join" element={<JoinPage />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default App;
